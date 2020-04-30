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
import graphql.schema.idl.TypeRuntimeWiring;
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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private Map<String, Map<String, DataFetcher>> wfResolvers;


    @PostConstruct
    void init() {
        if(graphqlProperties.isLog()) {
            logger.info("init GraphQL start");
        }
        ClassPathScanningCandidateComponentProvider cp = new ClassPathScanningCandidateComponentProvider(false);
        cp.addIncludeFilter(new AnnotationTypeFilter(Query.class));
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
                Query query = (Query)c.getAnnotation(Query.class);
                String value = query.value();
                if (StringUtils.isEmpty(value)) {
                    value = "Query";
                }

                Method[] methods = c.getDeclaredMethods();
                for(Method method: methods) {
                    method.setAccessible(true);
                    if (method.isAnnotationPresent(QueryMethod.class)) {
                        String methodQueryValue = value;
                        if (method.isAnnotationPresent(Query.class)) {
                            Query q = method.getAnnotation(Query.class);
                            methodQueryValue = StringUtils.isEmpty(q.value()) ? value : q.value();
                        }
                        Map<String, DataFetcher> resolverMap = resolvers.get(methodQueryValue);
                        Map<String, Class<?>> resolverClassMap = originClassResolvers.get(methodQueryValue);

                        if (resolverMap == null) {
                            resolverMap = new LinkedHashMap<>();
                        }
                        if (resolverClassMap == null) {
                           resolverClassMap = new LinkedHashMap<>();
                        }


                        QueryMethod qm = method.getAnnotation(QueryMethod.class);
                        String methodValue = qm.value();
                        if (StringUtils.isEmpty(methodValue)) {
                            methodValue = method.getName();
                        }
                        Annotation[][] ats = method.getParameterAnnotations();
                        Class<?>[] clsTypes = method.getParameterTypes();
                        DataFetcher df = dataFetchingEnvironment -> {
                            GraphQLContextUtil.add(dataFetchingEnvironment);
                            List<Object> list = new ArrayList<>();
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
                            return method.invoke(this.context.getBean(c), list.toArray(new Object[list.size()]));
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
            logger.info("init GraphQL end.");
        }
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
