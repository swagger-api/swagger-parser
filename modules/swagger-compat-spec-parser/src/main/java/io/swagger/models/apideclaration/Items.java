package io.swagger.models.apideclaration;

/**
 * Created by ron on 17/04/14.
 */
public class Items extends TypedObject {
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Items {\n");
        sb.append("  type: ").append(getType()).append("\n");
        sb.append("  format: ").append(getFormat()).append("\n");
        sb.append("  $ref: ").append(getRef()).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
