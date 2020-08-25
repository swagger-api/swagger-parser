package io.swagger.parser.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.models.AbstractModel;
import io.swagger.models.Xml;
import io.swagger.models.properties.Property;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlType;

@XmlType(
        propOrder = {"type", "required", "discriminator", "properties"}
)
@JsonPropertyOrder({"type", "required", "discriminator", "properties"})
public class ModelImpl extends AbstractModel {
    public static final String OBJECT = "object";
    private String type;
    private String format;
    private String name;
    private Boolean allowEmptyValue;
    private Boolean uniqueItems;
    private boolean isSimple = false;
    private String description;
    private Object example;
    private Object additionalProperties;
    private String discriminator;
    @JsonProperty("default")
    private String defaultValue;
    private List<String> _enum;
    private BigDecimal minimum;
    private BigDecimal maximum;

    public ModelImpl() {
    }

    public ModelImpl _enum(List<String> value) {
        this._enum = value;
        return this;
    }

    public ModelImpl _enum(String value) {
        if (this._enum == null) {
            this._enum = new ArrayList();
        }

        this._enum.add(value);
        return this;
    }

    public List<String> getEnum() {
        return this._enum;
    }

    public void setEnum(List<String> _enum) {
        this._enum = _enum;
    }

    public ModelImpl discriminator(String discriminator) {
        this.setDiscriminator(discriminator);
        return this;
    }

    public ModelImpl type(String type) {
        this.setType(type);
        return this;
    }

    public ModelImpl format(String format) {
        this.setFormat(format);
        return this;
    }

    public ModelImpl name(String name) {
        this.setName(name);
        return this;
    }

    public ModelImpl uniqueItems(Boolean uniqueItems) {
        this.setUniqueItems(uniqueItems);
        return this;
    }

    public ModelImpl allowEmptyValue(Boolean allowEmptyValue) {
        this.setAllowEmptyValue(allowEmptyValue);
        return this;
    }

    public ModelImpl description(String description) {
        this.setDescription(description);
        return this;
    }

    public ModelImpl property(String key, Property property) {
        this.addProperty(key, property);
        return this;
    }

    public ModelImpl example(Object example) {
        this.setExample(example);
        return this;
    }

    public ModelImpl additionalProperties(Object additionalProperties) {
        this.setAdditionalProperties(additionalProperties);
        return this;
    }

    public ModelImpl required(String name) {
        this.addRequired(name);
        return this;
    }

    public ModelImpl xml(Xml xml) {
        this.setXml(xml);
        return this;
    }

    public ModelImpl minimum(BigDecimal minimum) {
        this.minimum = minimum;
        return this;
    }

    public ModelImpl maximum(BigDecimal maximum) {
        this.maximum = maximum;
        return this;
    }

    public String getDiscriminator() {
        return this.discriminator;
    }

    public void setDiscriminator(String discriminator) {
        this.discriminator = discriminator;
    }

    @JsonIgnore
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonIgnore
    public boolean isSimple() {
        return this.isSimple;
    }

    public void setSimple(boolean isSimple) {
        this.isSimple = isSimple;
    }

    public Object getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperties(Object additionalProperties) {
        this.type("object");
        this.additionalProperties = additionalProperties;
    }

    public Boolean getAllowEmptyValue() {
        return this.allowEmptyValue;
    }

    public void setAllowEmptyValue(Boolean allowEmptyValue) {
        if (allowEmptyValue != null) {
            this.allowEmptyValue = allowEmptyValue;
        }

    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void addRequired(String name) {
        if (this.required == null) {
            this.required = new ArrayList();
        }

        this.required.add(name);
        Property p = (Property)this.properties.get(name);
        if (p != null) {
            p.setRequired(true);
        }

    }

    public Object getExample() {
        return this.example;
    }

    public void setExample(Object example) {
        this.example = example;
    }

    public Object getDefaultValue() {
        if (this.defaultValue == null) {
            return null;
        } else {
            try {
                if ("integer".equals(this.type)) {
                    return new Integer(this.defaultValue);
                }

                if ("number".equals(this.type)) {
                    return new BigDecimal(this.defaultValue);
                }
            } catch (Exception var2) {
                return null;
            }

            return this.defaultValue;
        }
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public BigDecimal getMinimum() {
        return this.minimum;
    }

    public void setMinimum(BigDecimal minimum) {
        this.minimum = minimum;
    }

    public BigDecimal getMaximum() {
        return this.maximum;
    }

    public void setMaximum(BigDecimal maximum) {
        this.maximum = maximum;
    }

    public Boolean getUniqueItems() {
        return this.uniqueItems;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ModelImpl)) {
            return false;
        } else if (!super.equals(o)) {
            return false;
        } else {
            ModelImpl model = (ModelImpl)o;
            if (this.isSimple != model.isSimple) {
                return false;
            } else {
                if (this.type != null) {
                    if (!this.type.equals(model.type)) {
                        return false;
                    }
                } else if (model.type != null) {
                    return false;
                }

                if (this.format != null) {
                    if (!this.format.equals(model.format)) {
                        return false;
                    }
                } else if (model.format != null) {
                    return false;
                }

                label151: {
                    if (this.name != null) {
                        if (this.name.equals(model.name)) {
                            break label151;
                        }
                    } else if (model.name == null) {
                        break label151;
                    }

                    return false;
                }

                label144: {
                    if (this.allowEmptyValue != null) {
                        if (this.allowEmptyValue.equals(model.allowEmptyValue)) {
                            break label144;
                        }
                    } else if (model.allowEmptyValue == null) {
                        break label144;
                    }

                    return false;
                }

                if (this.uniqueItems != null) {
                    if (!this.uniqueItems.equals(model.uniqueItems)) {
                        return false;
                    }
                } else if (model.uniqueItems != null) {
                    return false;
                }

                if (this.description != null) {
                    if (!this.description.equals(model.description)) {
                        return false;
                    }
                } else if (model.description != null) {
                    return false;
                }

                label123: {
                    if (this.example != null) {
                        if (this.example.equals(model.example)) {
                            break label123;
                        }
                    } else if (model.example == null) {
                        break label123;
                    }

                    return false;
                }

                if (this.additionalProperties != null) {
                    if (!this.additionalProperties.equals(model.additionalProperties)) {
                        return false;
                    }
                } else if (model.additionalProperties != null) {
                    return false;
                }

                if (this.discriminator != null) {
                    if (!this.discriminator.equals(model.discriminator)) {
                        return false;
                    }
                } else if (model.discriminator != null) {
                    return false;
                }

                label102: {
                    if (this.defaultValue != null) {
                        if (this.defaultValue.equals(model.defaultValue)) {
                            break label102;
                        }
                    } else if (model.defaultValue == null) {
                        break label102;
                    }

                    return false;
                }

                label95: {
                    if (this._enum != null) {
                        if (this._enum.equals(model._enum)) {
                            break label95;
                        }
                    } else if (model._enum == null) {
                        break label95;
                    }

                    return false;
                }

                if (this.minimum != null) {
                    if (this.minimum.equals(model.minimum)) {
                        return this.maximum != null ? this.maximum.equals(model.maximum) : model.maximum == null;
                    }
                } else if (model.minimum == null) {
                    return this.maximum != null ? this.maximum.equals(model.maximum) : model.maximum == null;
                }

                return false;
            }
        }
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (this.type != null ? this.type.hashCode() : 0);
        result = 31 * result + (this.format != null ? this.format.hashCode() : 0);
        result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
        result = 31 * result + (this.allowEmptyValue != null ? this.allowEmptyValue.hashCode() : 0);
        result = 31 * result + (this.uniqueItems != null ? this.uniqueItems.hashCode() : 0);
        result = 31 * result + (this.isSimple ? 1 : 0);
        result = 31 * result + (this.description != null ? this.description.hashCode() : 0);
        result = 31 * result + (this.example != null ? this.example.hashCode() : 0);
        result = 31 * result + (this.additionalProperties != null ? this.additionalProperties.hashCode() : 0);
        result = 31 * result + (this.discriminator != null ? this.discriminator.hashCode() : 0);
        result = 31 * result + (this.defaultValue != null ? this.defaultValue.hashCode() : 0);
        result = 31 * result + (this._enum != null ? this._enum.hashCode() : 0);
        result = 31 * result + (this.minimum != null ? this.minimum.hashCode() : 0);
        result = 31 * result + (this.maximum != null ? this.maximum.hashCode() : 0);
        return result;
    }

    public void setUniqueItems(Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
    }

    public Object clone() {
        ModelImpl cloned = new ModelImpl();
        super.cloneTo(cloned);
        cloned.type = this.type;
        cloned.name = this.name;
        cloned.isSimple = this.isSimple;
        cloned.description = this.description;
        cloned.example = this.example;
        cloned.additionalProperties = this.additionalProperties;
        cloned.discriminator = this.discriminator;
        cloned.defaultValue = this.defaultValue;
        cloned.minimum = this.minimum;
        cloned.maximum = this.maximum;
        return cloned;
    }
}
