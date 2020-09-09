package cc.voox.graphql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryMethod {
    /**
     * if value empty, will use method name as value
     * @return
     */
    String value() default "";

    /**
     * If type empty, will use @Query value as type,
     * if current class not @Query annotated, will use simple class name as type
     * @return
     */
    String type() default "";
}
