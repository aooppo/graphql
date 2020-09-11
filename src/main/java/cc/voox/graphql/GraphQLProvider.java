package cc.voox.graphql;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;

@Component
@ComponentScan(value = "graphql.spring.web.servlet", excludeFilters = @ComponentScan.Filter(classes = {RestController.class}))
public class GraphQLProvider {

    @Autowired
    private GraphqlResolverFactory graphqlResolverFactory;
    @Autowired
    private GraphqlProperties graphqlProperties;

    private GraphQL graphQL;

    @PostConstruct
    public void init() throws IOException {
        URL url = Resources.getResource(graphqlProperties.getSchema());
        String sdl = Resources.toString(url, Charsets.UTF_8);
        GraphQLSchema graphQLSchema = buildSchema(sdl);
        DataLoaderDispatcherInstrumentationOptions options = DataLoaderDispatcherInstrumentationOptions
                .newOptions().includeStatistics(graphqlProperties.isOpenStatistics());

        DataLoaderDispatcherInstrumentation dispatcherInstrumentation
                = new DataLoaderDispatcherInstrumentation(options);
        MaxQueryDepthInstrumentation maxQueryDepthInstrumentation = new MaxQueryDepthInstrumentation(graphqlProperties.getMaxQueryDepth());
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).instrumentation(dispatcherInstrumentation).instrumentation(maxQueryDepthInstrumentation).build();
    }

    private GraphQLSchema buildSchema(String sdl) {
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        RuntimeWiring runtimeWiring = buildWiring();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
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

//        graphqlResolverFactory.getBuilders().forEach(b -> rb.fieldVisibility());
        return rb.build();
    }

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }

}
