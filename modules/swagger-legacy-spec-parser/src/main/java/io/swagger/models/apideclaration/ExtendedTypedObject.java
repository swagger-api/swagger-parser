package io.swagger.models.apideclaration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by ron on 17/04/14.
 */
public abstract class ExtendedTypedObject extends TypedObject {
    private String defaultValue;
    private List<String> enumValues;
    private String minimum;
    private String maximum;
    private Items items;
    private Boolean uniqueItems;

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @JsonProperty("enum")
    public List<String> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
    }

    public String getMinimum() {
        return minimum;
    }

    public void setMinimum(String minimum) {
        this.minimum = minimum;
    }

    public String getMaximum() {
        return maximum;
    }

    public void setMaximum(String maximum) {
        this.maximum = maximum;
    }

    public Items getItems() {
        return items;
    }

    public void setItems(Items items) {
        this.items = items;
    }

    public Boolean getUniqueItems() {
        return uniqueItems;
    }

    public void setUniqueItems(Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
    }
}
