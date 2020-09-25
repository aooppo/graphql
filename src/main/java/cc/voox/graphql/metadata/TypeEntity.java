package cc.voox.graphql.metadata;

import java.util.List;

public class TypeEntity {
    private String name;
    private String description;
    private boolean inputType;
    private boolean enumType;
    private List<TypeField> typeField;
    private List<Object> values;

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }

    public boolean isEnumType() {
        return enumType;
    }

    public void setEnumType(boolean enumType) {
        this.enumType = enumType;
    }

    public List<TypeField> getTypeField() {
        return typeField;
    }

    public void setTypeField(List<TypeField> typeField) {
        this.typeField = typeField;
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

    public boolean isInputType() {
        return inputType;
    }

    public void setInputType(boolean inputType) {
        this.inputType = inputType;
    }

    @Override
    public String toString() {
        return "TypeEntity{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", inputType=" + inputType +
                ", typeField=" + typeField +
                '}';
    }
}
