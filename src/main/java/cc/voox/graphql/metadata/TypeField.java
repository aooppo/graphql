package cc.voox.graphql.metadata;

import graphql.schema.GraphQLArgument;

import java.util.LinkedList;
import java.util.List;

public class TypeField<T> {
    private String value;

    private String description;

    private T type;
    private List<GraphQLArgument> arguments = new LinkedList<>();

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public T getType() {
        return type;
    }

    public void setType(T type) {
        this.type = type;
    }

    public List<GraphQLArgument> getArguments() {
        return arguments;
    }

    void setArguments(List<GraphQLArgument> arguments) {
        this.arguments = arguments;
    }

    public void addArgument(GraphQLArgument argument) {
        this.arguments.add(argument);
    }

    @Override
    public String toString() {
        return "TypeField{" +
                "value='" + value + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                '}';
    }
}
