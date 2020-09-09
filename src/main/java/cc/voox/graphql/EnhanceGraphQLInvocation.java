package cc.voox.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.spring.web.servlet.GraphQLInvocationData;
import graphql.spring.web.servlet.components.DefaultGraphQLInvocation;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Primary
@Component
public class EnhanceGraphQLInvocation extends DefaultGraphQLInvocation {
    @Autowired
    private GraphQL graphQL;

    @Autowired
    private GraphqlResolverFactory graphqlResolverFactory;

    @Override
    public CompletableFuture<ExecutionResult> invoke(GraphQLInvocationData invocationData, WebRequest webRequest) {
        //
        // a batch loader function that will be called with N or more keys for batch loading
        // This can be a singleton object since it's stateless
        //
//        BatchLoader<String, Object> characterBatchLoader = keys -> {
//            //
//            // we use supplyAsync() of values here for maximum parellisation
//            //TODO
//            return CompletableFuture.supplyAsync(() -> {
//                ArrayList<Object> objects = new ArrayList<>();
//                objects.add(keys);
//                System.out.println("mock ~ exec sql query with key"+ keys.toString());
//                return objects;});
//        };

//        DataLoader<String, Object> characterDataLoader = DataLoader.newDataLoader(characterBatchLoader);
        DataLoaderRegistry registry = new DataLoaderRegistry();
        graphqlResolverFactory.getDataLoaders().forEach(registry::register);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(invocationData.getQuery()).operationName(invocationData.getOperationName()).variables(invocationData.getVariables())
                .dataLoaderRegistry(registry)
                .build();
        return this.graphQL.executeAsync(executionInput);
    }
}
