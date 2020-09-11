![Travis (.org)](https://img.shields.io/travis/aooppo/graphql)
![example:](https://github.com/aooppo/graphql-java-example)
# Install
- add dependencies into pom.xml
``` xml
<dependency>
    <groupId>cc.voox</groupId>
    <artifactId>graphql</artifactId>
    <version>0.8.1</version>
</dependency>
```


-  add config into java bean
```java
import cc.voox.graphql.GraphqlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan(value = {"cc.voox.graphql"})
public class GraphQLConfig {
    @Bean()
    public GraphqlProperties graphqlProperties() {
        GraphqlProperties graphqlProperties = new GraphqlProperties();
        graphqlProperties.setScanPath("com.xxx");
        return graphqlProperties;
    }

}

```

# Usage
 - define schema in classpath  or custom schema path value (default value: schema.graphql)   
```graphql
    type Query {
         test: String
    }
    type Book {
        id: ID
        name: String
        pageCount: Int
    }
```
####  define resolvers
 - use @Query in entity class or method
 - use @QueryMethod in method 
 - use @QueryField in parameter of method
```java
@Query
@Component
public class TestService { 
        @QueryMethod("bookById")
        Map<String, String> getBookById(@QueryField("id") String id) {
            return books
                        .stream()
                        .filter(book -> book.get("id").equals(id))
                        .findFirst()
                        .orElse(null);
        }
}
``` 

### define scalars
```java 
class Currency implements IScalar {
} 
```
### define directive
```java 
class Lower implements IDirective {
}
```
### define dataloader
```java 
class BookDatalodaer implements IDataloader {
}
```
