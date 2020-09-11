package cc.voox.graphql.autoconfiguration;

import cc.voox.graphql.GraphqlProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GraphqlProperties.class)
@ComponentScan(value = {"cc.voox.graphql"})
public class GraphQLAutoconfiguration {

    @Bean()
    public GraphqlProperties graphqlProperties() {
        GraphqlProperties graphqlProperties = new GraphqlProperties();

        graphqlProperties.setMapping("/altair");
        graphqlProperties.setLog(true);
        graphqlProperties.getEndpoint().setGraphql("/graphql");
        graphqlProperties.getEndpoint().setSubscriptions("/graphql/subscriptions");
        return graphqlProperties;
    }
}
