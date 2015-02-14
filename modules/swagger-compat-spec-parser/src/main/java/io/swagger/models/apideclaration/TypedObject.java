package io.swagger.models.apideclaration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.models.Format;
import io.swagger.models.SwaggerBaseModel;

/**
 * Created by ron on 17/04/14.
 */
public abstract class TypedObject extends SwaggerBaseModel {
    private String type;
    private String ref;
    private Format format;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("$ref")
    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }
}
