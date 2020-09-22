package cc.voox.graphql.annotation;

import cc.voox.graphql.IDirective;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Repeatable(value = Directives.class)
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Directive {
    Class<? extends IDirective> value();
    String[] params() default {};
}

@Target({ElementType.METHOD})
@Retention( RetentionPolicy.RUNTIME )
@interface Directives {
    Directive[] value() default{};
}


