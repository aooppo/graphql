package cc.voox.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.Internal;
import graphql.spring.web.servlet.ExecutionResultHandler;
import graphql.spring.web.servlet.GraphQLInvocation;
import graphql.spring.web.servlet.GraphQLInvocationData;
import graphql.spring.web.servlet.components.GraphQLRequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@Internal
public class GraphQLConfigController {
    @Autowired
    private GraphQLInvocation graphQLInvocation;
    @Autowired
    private ExecutionResultHandler executionResultHandler;
    @Autowired
    private ObjectMapper objectMapper;

    public GraphQLConfigController() {
    }
    @CrossOrigin
    @RequestMapping(
            value = "graphql",
            method = {RequestMethod.POST},
            consumes = {"application/json"},
            produces = {"application/json;charset=UTF-8"}
    )
    public Object graphqlPOST(@RequestBody GraphQLRequestBody body, WebRequest webRequest) {
        String query = body.getQuery();
        if (query == null) {
            query = "";
        }

        CompletableFuture<ExecutionResult> executionResult = this.graphQLInvocation.invoke(new GraphQLInvocationData(query, body.getOperationName(), body.getVariables()), webRequest);
        return this.executionResultHandler.handleExecutionResult(executionResult);
    }
    @CrossOrigin
    @RequestMapping(
            value = "graphql",
            method = {RequestMethod.GET},
            produces = {"application/json;charset=UTF-8"}
    )
    public Object graphqlGET(@RequestParam("query") String query, @RequestParam(value = "operationName",required = false) String operationName, @RequestParam(value = "variables",required = false) String variablesJson, WebRequest webRequest) {
        CompletableFuture<ExecutionResult> executionResult = this.graphQLInvocation.invoke(new GraphQLInvocationData(query, operationName, this.convertVariablesJson(variablesJson)), webRequest);
        return this.executionResultHandler.handleExecutionResult(executionResult);
    }

    private Map<String, Object> convertVariablesJson(String jsonMap) {
        if (jsonMap == null) {
            return Collections.emptyMap();
        } else {
            try {
                return (Map)this.objectMapper.readValue(jsonMap, Map.class);
            } catch (IOException var3) {
                throw new RuntimeException("Could not convert variables GET parameter: expected a JSON map", var3);
            }
        }
    }
}
