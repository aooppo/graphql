package cc.voox.graphql.utils;

import cc.voox.graphql.GraphQLContextUtil;
import cc.voox.graphql.IDirective;
import cc.voox.graphql.annotation.Directive;
import cc.voox.graphql.annotation.QueryField;
import cc.voox.graphql.metadata.TypeArgument;
import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLScalarType;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.Scalars.GraphQLString;

public class GraphQLTypeUtils {

    public static List<TypeArgument> getArguments(Method method) throws Exception {
        if (method == null) {
            throw new RuntimeException("Method is null.");
        }
        List<TypeArgument> list = new ArrayList<>();
        Annotation[][] ats = method.getParameterAnnotations();
        for (Annotation[] as : ats) {

            for (Annotation a : as) {
                QueryField queryField = AnnotationUtils.getAnnotation(a, QueryField.class);
                if (queryField != null) {
                    TypeArgument typeArgument = new TypeArgument();
                    typeArgument.setName(queryField.value());
                    typeArgument.setRequired(queryField.required());
                    typeArgument.setDescription(queryField.description());
                    typeArgument.setInputType(queryField.type());
                    typeArgument.setRoot(queryField.root());
                    list.add(typeArgument);
                }
            }
        }
        return  list;
    }

    public static LinkedList<String> getTypesFromIndex(Method method, int index) {
        if(method == null) return null;
        List<LinkedList<String>> list = new LinkedList<>();
        method.setAccessible(true);
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        for (int i = 0; i < genericParameterTypes.length; i++) {
            if( genericParameterTypes[i] instanceof ParameterizedType) {
                Type[] parameters = ((ParameterizedType)genericParameterTypes[i]).getActualTypeArguments();
                LinkedList<String> objects = new LinkedList<>();
                for (Type parameter : parameters) {
//                    System.out.println(parameter);
                    objects.add(parameter.getTypeName());
                }
                list.add(objects);
//                System.out.println("~~~~~~");
            }
        }
        return list.get(index);
    }

    public static List<Class<?>> getArgumentType(Method method) throws Exception {
        if (method == null) {
            throw new RuntimeException("Method is null.");
        }
        List<Class<?>> list = new ArrayList<>();
        Annotation[][] ats = method.getParameterAnnotations();
        Class<?>[] clsTypes = method.getParameterTypes();
        DataFetchingEnvironment dataFetchingEnvironment = GraphQLContextUtil.get();
        if (dataFetchingEnvironment == null) {
            throw new RuntimeException("Current thread doesn't have resolver context.");
        }
        int i = 0;
        for (Annotation[] as : ats) {

            for (Annotation a : as) {
                QueryField queryField = AnnotationUtils.getAnnotation(a, QueryField.class);
                if (queryField != null) {
                    String qfv = queryField.value();
                    boolean fromSource = queryField.source();
                    boolean root = queryField.root();
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
                            /**
                             * get parent sub field
                             */
                            val = BeanUtil.getFieldValue(o, qfv);
                        }
//                        list.add(val);
                        break;
                    } else if (root) {
                        list.add(dataFetchingEnvironment.getSource());
                        break;
                    } else {

                        Object v = dataFetchingEnvironment.getArgument(qfv);
                        Class<?> clz = clsTypes[i];
//                        list.add(v);
                        break;
                    }
                }
            }
            i++;
        }
        return list;
    }

    public static GraphQLScalarType getType(Class<?> fieldTypeClass) {
        if(fieldTypeClass == null) {
            return null;
        }
        if (fieldTypeClass.equals(String.class)) {
            return GraphQLString;
        } else if (fieldTypeClass.equals(short.class)) {
            return Scalars.GraphQLShort;
        } else if (fieldTypeClass.equals(BigDecimal.class)) {
            return Scalars.GraphQLBigDecimal;
        } else if (fieldTypeClass.equals(BigInteger.class)) {
            return Scalars.GraphQLBigInteger;
        } else if (fieldTypeClass.equals(Boolean.class) || fieldTypeClass.equals(boolean.class)) {
            return Scalars.GraphQLBoolean;
        } else if (fieldTypeClass.equals(Byte.class) || fieldTypeClass.equals(byte.class)) {
            return Scalars.GraphQLByte;
        } else if (fieldTypeClass.equals(Character.class) || fieldTypeClass.equals(char.class)) {
            return Scalars.GraphQLChar;
        } else if (fieldTypeClass.equals(Float.class) || fieldTypeClass.equals(float.class)) {
            return Scalars.GraphQLFloat;
        } else if (fieldTypeClass.equals(Integer.class) || fieldTypeClass.equals(int.class)) {
            return Scalars.GraphQLInt;
        } else if (fieldTypeClass.equals(Long.class) || fieldTypeClass.equals(long.class)) {
            return Scalars.GraphQLLong;
        }
        return null;
    }

    public static boolean isGraphQLPrimitive(Class<?> fieldType) {
        List<Class<?>> objects = Arrays.asList(String.class, Double.class, Integer.class, Boolean.class, Long.class,BigInteger.class, BigDecimal.class, Float.class);
        return objects.contains(fieldType);
    }

    public static List<Class<? extends IDirective>> getDirectives(Method method) {
        if (method == null) {
            return Collections.emptyList();
        } else {
            if (method.isAnnotationPresent(Directive.class)) {
                Directive[] annotationsByType = method.getAnnotationsByType(Directive.class);
                return Stream.of(annotationsByType).map(Directive::value).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        }
    }
}
