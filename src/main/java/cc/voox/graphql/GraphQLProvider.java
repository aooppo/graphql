package cc.voox.graphql;

import cc.voox.graphql.annotation.ObjectField;
import cc.voox.graphql.annotation.ObjectType;
import cc.voox.graphql.metadata.TypeEntity;
import cc.voox.graphql.metadata.TypeField;
import cc.voox.graphql.utils.GraphQLTypeUtils;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphqlTypeBuilder;
import graphql.schema.TypeResolver;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static cc.voox.graphql.utils.GraphQLTypeUtils.isGraphQLPrimitive;

@Component
@ComponentScan(value = "graphql.spring.web.servlet", excludeFilters = @ComponentScan.Filter(classes = {RestController.class}))
public class GraphQLProvider {

    @Autowired
    private GraphqlResolverFactory graphqlResolverFactory;
    @Autowired
    private GraphqlProperties graphqlProperties;

    private GraphQL graphQL;
    private List<TypeEntity> typeEntityList = new ArrayList<>();
//    private Set<GraphQLType> additionalTypes = new HashSet<>();
    private Map<String, GraphqlTypeBuilder> additionalTypesBuilders = new HashMap<>();

    void initSchema() {
        if (!graphqlProperties.isEnableCodeMode()) {
            return;
        }
        List<Class<?>> typeList = graphqlResolverFactory.getTypeList();
        for (Class<?> clz : typeList) {
            ObjectType objectType = clz.getAnnotation(ObjectType.class);
            TypeEntity typeEntity = convert(objectType, clz);
            typeEntityList.add(typeEntity);
        }
        typeEntityList.stream().forEach(typeEntity -> {

            if (typeEntity.isInputType()) {
                GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject()
                        .name(typeEntity.getName())
                        .description(typeEntity.getDescription());

                typeEntity.getTypeField().stream().forEach(typeField -> {
                    GraphQLInputObjectField.Builder graphQLFieldDefinition = GraphQLInputObjectField.newInputObjectField()
                            .name(typeField.getValue())
                            .description(typeField.getDescription());
                    Object typeFieldType = typeField.getType();
                    if (typeFieldType instanceof GraphQLInputObjectType) {
                        graphQLFieldDefinition.type((GraphQLInputObjectType) typeFieldType);
                    } else if (typeFieldType instanceof GraphQLScalarType) {
                        graphQLFieldDefinition.type((GraphQLScalarType) typeFieldType);
                    } else if (typeFieldType instanceof GraphQLTypeReference) {
                        graphQLFieldDefinition.type((GraphQLTypeReference) typeFieldType);
                    }
                    builder.field(graphQLFieldDefinition.build());
                });
                additionalTypesBuilders.put(typeEntity.getName(), builder);
            } else if(typeEntity.isEnumType()) {
                GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum()
                        .name(typeEntity.getName())
                        .description(typeEntity.getDescription());

                typeEntity.getTypeField().stream().forEach(typeField -> {



                });
            }else {
                GraphQLObjectType.Builder builder = GraphQLObjectType.newObject()
                        .name(typeEntity.getName())
                        .description(typeEntity.getDescription());

                typeEntity.getTypeField().stream().forEach(typeField -> {
                    GraphQLFieldDefinition.Builder graphQLFieldDefinition = GraphQLFieldDefinition.newFieldDefinition()
                            .name(typeField.getValue())
                            .description(typeField.getDescription());
                    Object typeFieldType = typeField.getType();
                    if (typeFieldType instanceof GraphQLObjectType) {
                        graphQLFieldDefinition.type((GraphQLObjectType) typeFieldType);
                    } else if (typeFieldType instanceof GraphQLScalarType) {
                        graphQLFieldDefinition.type((GraphQLScalarType) typeFieldType);
                    } else if (typeFieldType instanceof GraphQLTypeReference) {
                        graphQLFieldDefinition.type((GraphQLTypeReference) typeFieldType);
                    }
                    builder.field(graphQLFieldDefinition.build());

                });
                additionalTypesBuilders.put(typeEntity.getName(), builder);
            }
        });
    }

    private TypeEntity convert(ObjectType objectType, Class<?> clz) {
        TypeEntity typeEntity = new TypeEntity();
        String value = objectType.value();
        if (StringUtils.isEmpty(value)) {
            value = clz.getSimpleName();
        }
        typeEntity.setName(value);
        typeEntity.setDescription(objectType.description());
        typeEntity.setInputType(objectType.inputType());
        List<TypeField> typeFields = initFields(clz);
        typeEntity.setTypeField(typeFields);
        return typeEntity;
    }

    private List<TypeField> initFields(Class<?> clz) {
        List<TypeField> typeFields = new ArrayList<>();
        ReflectionUtils.doWithFields(clz, field -> {
                    field.setAccessible(true);
                    ObjectField objectField = field.getAnnotation(ObjectField.class);
                    TypeField typeField = convertField(objectField, field);
                    typeFields.add(typeField);
                }, field -> field.isAnnotationPresent(ObjectField.class)
        );
        return typeFields;
    }

    private TypeField convertField(ObjectField objectField, Field field) {
        TypeField typeField = new TypeField();
        typeField.setDescription(objectField.description());
        String value = objectField.value();
        if (StringUtils.isEmpty(value)) {
            value = field.getName();
        }
        typeField.setValue(value);
        Class<?> fieldType = field.getType();
        boolean isPrimitive = fieldType.isPrimitive();
        if (isPrimitive || isGraphQLPrimitive(fieldType)) {
            if (objectField.id()) {
                typeField.setType(Scalars.GraphQLID);
            } else {
                GraphQLScalarType objectFieldType = GraphQLTypeUtils.getType(fieldType);
                typeField.setType(objectFieldType);
            }
        } else {
            if (Collection.class.isAssignableFrom(fieldType)) {
                ParameterizedType typeListType = (ParameterizedType) field.getGenericType();
                Class<?> typeListClass = (Class<?>) typeListType.getActualTypeArguments()[0];
                GraphQLScalarType objectFieldType = GraphQLTypeUtils.getType(typeListClass);
                if (objectFieldType != null) {
                    typeField.setType(GraphQLList.list(objectFieldType));
                } else {
                    typeField.setType(GraphQLList.list(GraphQLTypeReference.typeRef(typeListClass.getSimpleName())));
                }
            } else {
                typeField.setType(GraphQLTypeReference.typeRef(fieldType.getSimpleName()));
            }
        }

        return typeField;
    }




    @PostConstruct
    public void init() throws IOException {
        GraphQLSchema graphQLSchema = null;
        if(graphqlProperties.isEnableCodeMode()) {
            initSchema();
            graphQLSchema = buildSchema();
        } else {
            URL url = Resources.getResource(graphqlProperties.getSchema());
            String sdl = Resources.toString(url, Charsets.UTF_8);
            graphQLSchema = buildSchema(sdl);
        }
        DataLoaderDispatcherInstrumentationOptions options = DataLoaderDispatcherInstrumentationOptions
                .newOptions().includeStatistics(graphqlProperties.isOpenStatistics());

        DataLoaderDispatcherInstrumentation dispatcherInstrumentation
                = new DataLoaderDispatcherInstrumentation(options);
        Set<GraphQLInterceptor> interceptors = graphqlResolverFactory.getInterceptors();
        List<GraphQLInterceptor> graphQLInterceptors = new ArrayList<>(interceptors);
        List<GraphQLInterceptor> collect = graphQLInterceptors.stream().sorted(Comparator.comparingInt(GraphQLInterceptor::getOrder)).collect(Collectors.toList());
        MaxQueryDepthInstrumentation maxQueryDepthInstrumentation = new MaxQueryDepthInstrumentation(graphqlProperties.getMaxQueryDepth());
//        MaxQueryComplexityInstrumentation maxQueryComplexityInstrumentation = new MaxQueryComplexityInstrumentation(100);
        List<Instrumentation> chainedList = new ArrayList<>();
        chainedList.add(dispatcherInstrumentation);
        chainedList.add(maxQueryDepthInstrumentation);
        chainedList.addAll(collect);
        ChainedInstrumentation chainedInstrumentation = new ChainedInstrumentation(chainedList);
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).instrumentation(chainedInstrumentation).build();
    }

    private GraphQLSchema buildSchema() {
        if(additionalTypesBuilders.size() > 0) {
            RuntimeWiring runtimeWiring = buildWiring();
            Map<String, Map<String, DataFetcher>> resolvers = graphqlResolverFactory.getResolvers();
            Map<String, Set<TypeField>> typeFieldMap = graphqlResolverFactory.getTypeFieldMap();
            GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema();
            Set<TypeField> queries = typeFieldMap.get("Query");
            Set<TypeField> mutations = typeFieldMap.get("Mutation");
            Map<String, DataFetcher> queryResolvers = resolvers.get("Query");
            Map<String, DataFetcher> mutationResolvers = resolvers.get("Mutation");

            GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("query");
            GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("mutation");

            setBuilder(queries, queryResolvers, queryBuilder);
            setBuilder(mutations, mutationResolvers, mutationBuilder);
            Set<String> keys = new HashSet<>();
            keys.add("Query");
            keys.add("Mutation");
            typeFieldMap.forEach((s, typeFields) -> {
                if(!Arrays.asList("Query", "Mutation").contains(s)) {
                    GraphqlTypeBuilder builder = additionalTypesBuilders.get(s);
                    if(builder instanceof  GraphQLInputObjectType.Builder) {
                        GraphQLInputObjectType.Builder extraBuilder = (GraphQLInputObjectType.Builder) builder;
//                        setBuilder(typeFields, resolvers.get(s), extraBuilder);
                        schemaBuilder.additionalType(extraBuilder.build());
                    } else {
                        GraphQLObjectType.Builder extraBuilder = (GraphQLObjectType.Builder) builder;
                        setBuilder(typeFields, resolvers.get(s), extraBuilder);
                        schemaBuilder.additionalType(extraBuilder.build());
                    }

                    keys.add(s);
//                    setBuilder(typeFields, resolvers.get(s), extraBuilder);

                }
            });
            additionalTypesBuilders.forEach((s, graphqlTypeBuilder) -> {
                if (!keys.contains(s)) {
                    if (graphqlTypeBuilder instanceof GraphQLObjectType.Builder) {
                        GraphQLObjectType.Builder builder = (GraphQLObjectType.Builder) graphqlTypeBuilder;
                        schemaBuilder.additionalType(builder.build());
                    } else if (graphqlTypeBuilder instanceof GraphQLInputObjectType.Builder) {
                        GraphQLInputObjectType.Builder builder = (GraphQLInputObjectType.Builder) graphqlTypeBuilder;
                        schemaBuilder.additionalType(builder.build());
                    }
                }
            });
            schemaBuilder.query(queryBuilder.build());
            schemaBuilder.mutation(mutationBuilder.build());

            return schemaBuilder.build();

        } else {
            throw new Error("No found graphql schema.");
        }
    }

    private void setBuilder(Set<TypeField> queries, Map<String, DataFetcher> dataFetcherMap, GraphQLObjectType.Builder queryBuilder) {
        queries.forEach(tf -> queryBuilder.field(builder -> {
            builder.name(tf.getValue());
            builder.type((GraphQLOutputType) tf.getType());
            builder.arguments(tf.getArguments());
//            GraphQLDirective.Builder b = GraphQLDirective.newDirective().
//            builder.withDirectives();
            builder.dataFetcher(dataFetcherMap.get(tf.getValue()));
            return builder;
        }).build());
    }

    private GraphQLSchema buildSchema(String sdl) {
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        RuntimeWiring runtimeWiring = buildWiring();
        GraphQLSchema graphQLSchema = null;
        if(!graphqlProperties.isEnableCodeMode()) {
            SchemaGenerator schemaGenerator = new SchemaGenerator();
            graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
        } else {
            throw new Error("No graphQL schema found.");
        }
        return graphQLSchema;
    }

    private RuntimeWiring buildWiring() {
        RuntimeWiring.Builder rb = RuntimeWiring.newRuntimeWiring();

        //build scalars
        graphqlResolverFactory.getScalarSet().forEach(s -> {
            rb.scalar(GraphQLScalarType.newScalar().name(s.getName()).description(s.getDescription()).coercing(s).build());
        });
        rb.scalar(ExtendedScalars.Date);
        rb.scalar(ExtendedScalars.Object);
        rb.scalar(ExtendedScalars.DateTime);
        rb.scalar(ExtendedScalars.Json);
        rb.scalar(ExtendedScalars.Url);
        //build directives
        graphqlResolverFactory.getDirectiveSet().forEach(d-> {
            rb.directive(d.getName(), d).build();
        });

        //build resolvers
        graphqlResolverFactory.getBuilders().forEach(b -> rb.type(b));

        return rb.build();
    }

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }

}
