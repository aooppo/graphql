package cc.voox.graphql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryField {
    /**
     * field name
     */
    String value();

    /**
     * Will get field value from root entity if source is true
     */
    boolean source() default false;

    /**
     * Will get root entity if root is true
     * if source is true, root is disable
     */
    boolean root() default false;

}
