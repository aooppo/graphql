package cc.voox.graphql;

import cc.voox.graphql.autoconfiguration.GraphQLAutoconfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(GraphQLAutoconfiguration.class)
public @interface EnableGraphQL {

}
