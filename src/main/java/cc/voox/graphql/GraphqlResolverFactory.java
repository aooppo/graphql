package cc.voox.graphql;

import cc.voox.graphql.annotation.Query;
import cc.voox.graphql.annotation.QueryField;
import cc.voox.graphql.annotation.QueryMethod;
import cc.voox.graphql.utils.AOPUtil;
import cc.voox.graphql.utils.BeanUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQLException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.TypeRuntimeWiring;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class GraphqlResolverFactory implements ApplicationContextAware {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private ApplicationContext context;

    @Autowired
    private GraphqlProperties graphqlProperties;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper responseMapper = new ObjectMapper();
        responseMapper.addMixIn(Object.class, JsonIgnore.class);
        responseMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        return responseMapper;
    }

    @Autowired
    private ObjectMapper objectMapper;
    private final Map<String, DataLoader<?, ?>> dataLoaders = new ConcurrentHashMap();
    private Map<String, Map<String, DataFetcher>> wfResolvers = new HashMap<>();
    private Set<IScalar> scalarSet = new HashSet<>();
    private Set<IDirective> directiveSet = new HashSet<>();

    protected Set<IScalar> getScalarSet() {
        return scalarSet;
    }
    protected Set<IDirective> getDirectiveSet() {
        return directiveSet;
    }

    protected Map<String, DataLoader<?, ?>> getDataLoaders() {
        return dataLoaders;
    }

    @PostConstruct
    void init() {
        if(graphqlProperties.isLog()) {
            logger.info("init GraphQL start");
        }
        ClassPathScanningCandidateComponentProvider cp = new ClassPathScanningCandidateComponentProvider(false);
        cp.addIncludeFilter(new AnnotationTypeFilter(Query.class));
        cp.addIncludeFilter(new AssignableTypeFilter(IGraphQL.class));
        cp.addIncludeFilter(new AssignableTypeFilter(IScalar.class));
        cp.addIncludeFilter(new AssignableTypeFilter(IDirective.class));
        cp.addIncludeFilter(new AssignableTypeFilter(IDataLoader.class));

        if (StringUtils.isEmpty(graphqlProperties.getScanPath())) {
           throw new GraphQLException("Scan path is empty. please set scan path in GraphqlProperties bean.");
        }
        Set<BeanDefinition> bd = cp.findCandidateComponents(graphqlProperties.getScanPath());
        ClassLoader loader = GraphqlResolverFactory.class.getClassLoader();
        Map<String, Map<String, DataFetcher>> resolvers = new HashMap();
        Map<String, Map<String, Class<?>>> originClassResolvers = new HashMap();
        Iterator var5 = bd.iterator();

        while(var5.hasNext()) {
            BeanDefinition b = (BeanDefinition)var5.next();
            String beanName = b.getBeanClassName();

            try {
                Class<?> c = loader.loadClass(beanName);
                Class<?>[] interfaces = c.getInterfaces();
                Set<Class<?>> set = new HashSet<>();
                for(Class<?> inter: interfaces) {
                    set.add(inter);
                }
                if (set.contains(IScalar.class) || IScalar.class.isAssignableFrom(c)) {
                    IScalar iScalar;
                    try {
                        iScalar = (IScalar) c.newInstance();
                    } catch (Exception e) {
                        throw new GraphQLException("Cannot initial scalar"+ c);
                    }
                    scalarSet.add(iScalar);
                    continue;
                } else if (set.contains(IDirective.class) || IDirective.class.isAssignableFrom(c)) {
                    IDirective directive;
                    try {
                        directive =(IDirective) c.newInstance();
                    } catch (Exception e) {
                        directive = (IDirective) this.context.getBean(c);
                        if (directive == null) {
                            throw new GraphQLException("Cannot initial directive"+ c);
                        }
                    }
                    directiveSet.add(directive);
                    continue;
                } else if (set.contains(IDataLoader.class) || IDataLoader.class.isAssignableFrom(c)) {
                    IDataLoader dataLoader;
                    try {
                        dataLoader = (IDataLoader) this.context.getBean(c);
                    } catch (Exception e) {
                        throw new GraphQLException("Cannot initial dataLoader"+ c);
                    }
                    dataLoaders.put(dataLoader.key(), dataLoader.get());
                    continue;
                }
                String queryValue = null;
                if(c.isAnnotationPresent(Query.class)) {
                    Query q = c.getAnnotation(Query.class);
                    queryValue = q.value();
                } else {
                    if(set.contains(IGraphQL.class) || IGraphQL.class.isAssignableFrom(c)) {
                        queryValue = c.getSimpleName();
                    }
                }
                String value = queryValue;
                if (StringUtils.isEmpty(value)) {
                    value = "Query";
                }

                Method[] methods = c.getDeclaredMethods();
                for(Method method: methods) {
                    method.setAccessible(true);
                    if (method.isAnnotationPresent(QueryMethod.class)) {
                        String methodQueryValue = value;
                        QueryMethod methodAnnotation = method.getAnnotation(QueryMethod.class);
                        String type = methodAnnotation.type();
                        String methodValue = methodAnnotation.value();
                        if (!StringUtils.isEmpty(type)) {
                            methodQueryValue = type;
                        } else if (method.isAnnotationPresent(Query.class)) {
                            Query q = method.getAnnotation(Query.class);
                            methodQueryValue = StringUtils.isEmpty(q.value()) ? value : q.value();
                        }  else {
                            methodQueryValue = value;
                        }
//                        final String resolverType = methodQueryValue;
                        Map<String, DataFetcher> resolverMap = resolvers.get(methodQueryValue);
                        Map<String, Class<?>> resolverClassMap = originClassResolvers.get(methodQueryValue);

                        if (resolverMap == null) {
                            resolverMap = new LinkedHashMap<>();
                        }
                        if (resolverClassMap == null) {
                           resolverClassMap = new LinkedHashMap<>();
                        }


                        if (StringUtils.isEmpty(methodValue)) {
                            methodValue = method.getName();
                        }

                        DataFetcher df = dataFetchingEnvironment -> {
                            GraphQLContextUtil.add(dataFetchingEnvironment);

                            Class<?>[] clsTypes = method.getParameterTypes();
                            List<Object> list = clsTypes.length >0? getArguments(method): Collections.emptyList();
                            try {
                                Object source = GraphQLContextUtil.get().getSource();
                                Object obj = IGraphQL.class.isAssignableFrom(c) && source != null? source: this.context.getBean(c);
                                return method.invoke(obj, list.toArray(new Object[list.size()]));
                            } finally {
                              logger.debug("clear context for graphql.");
                              GraphQLContextUtil.clear();
                            }
                        };
                        resolverClassMap.put(methodValue, c);
                        resolverMap.put(methodValue, df);
                        originClassResolvers.put(methodQueryValue, resolverClassMap);
                        resolvers.put(methodQueryValue, resolverMap);
                    }
                }

            } catch (ClassNotFoundException var12) {
            }
        }
        this.wfResolvers = resolvers;
        if (graphqlProperties.isLog()) {
            if (originClassResolvers != null) {
                originClassResolvers.forEach((k,v) -> {
                    logger.info("GraphQL resolvers @"+ k + " ===>" + v );
                });
            }
            if (scalarSet != null) {
                scalarSet.forEach(s -> {
                    logger.info("GraphQL scalar @"+ s.getName() + " ===>" + s.getClass() );
                });
            }
            if (directiveSet != null) {
                directiveSet.forEach(directive -> {
                    logger.info("GraphQL directive @"+ directive.getName() + " ===>" + directive.getClass() );
                });
            } else {
                logger.info("GraphQL @directive is empty");
            }
            if (dataLoaders != null) {
                dataLoaders.forEach((key, dl) -> {
                    logger.info("GraphQL dataloader @"+ key + " ===>" + dl.getClass() );
                });
            } else {
                logger.info("GraphQL @directive is empty");
            }
            logger.info("init GraphQL end.");
        }
    }

    private List<Object> getArguments(Method method) throws Exception {
        if (method == null) {
            throw new RuntimeException("Method is null.");
        }
        List<Object> list = new ArrayList<>();
        Annotation[][] ats = method.getParameterAnnotations();
        Class<?>[] clsTypes = method.getParameterTypes();
        DataFetchingEnvironment dataFetchingEnvironment = GraphQLContextUtil.get();
        if (dataFetchingEnvironment == null) {
           throw new RuntimeException("Current thread doesn't have resolver context.");
        }
        int i = 0;
        for (Annotation[] as: ats) {

            for (Annotation a : as) {
                QueryField queryField = AnnotationUtils.getAnnotation(a, QueryField.class);
                if (queryField != null) {
                    String qfv = queryField.value();
                    boolean fromSource = queryField.source();
                    if (fromSource) {
                        Object o = dataFetchingEnvironment.getSource();
                        if (o == null) {
                            list.add(null);
                            break;
                        }
                        Object val = null;
                        if (o instanceof Map) {
                            Map mo = (Map) o;
                            val = mo.get(qfv);
                        } else {
                            o = AOPUtil.getTarget(o);
                            val = BeanUtil.getFieldValue(o, qfv);
                        }
                        list.add(val);
                        break;
                    } else {

                        Object v = dataFetchingEnvironment.getArgument(qfv);
                        Class<?> clz = clsTypes[i];
                        v = objectMapper.convertValue(v, clz);
                        list.add(v);
                        break;
                    }
                }
            }
            i++;
        }
        return list;
    }

    public List<TypeRuntimeWiring.Builder> getBuilders() {
        List<TypeRuntimeWiring.Builder> bs = new ArrayList<>();
        for(Map.Entry<String, Map<String, DataFetcher>> rs: this.wfResolvers.entrySet()) {
            TypeRuntimeWiring.Builder tb = newTypeWiring(rs.getKey());
            tb.dataFetchers(rs.getValue());
            bs.add(tb);
        }
        return bs;
    }
}
