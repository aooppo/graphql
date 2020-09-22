package cc.voox.graphql.metadata;

public class TypeArgument {
    private String name;

    private String description;
    private Class<?> inputType;
    private boolean required;
    private boolean root;

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Class<?> getInputType() {
        return inputType;
    }

    public void setInputType(Class<?> inputType) {
        this.inputType = inputType;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
