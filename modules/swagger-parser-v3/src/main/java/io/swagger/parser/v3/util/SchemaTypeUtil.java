package io.swagger.parser.v3.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.oas.models.media.BinarySchema;
import io.swagger.oas.models.media.BooleanSchema;
import io.swagger.oas.models.media.ByteArraySchema;
import io.swagger.oas.models.media.DateSchema;
import io.swagger.oas.models.media.DateTimeSchema;
import io.swagger.oas.models.media.EmailSchema;
import io.swagger.oas.models.media.IntegerSchema;
import io.swagger.oas.models.media.NumberSchema;
import io.swagger.oas.models.media.ObjectSchema;
import io.swagger.oas.models.media.PasswordSchema;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.media.StringSchema;
import io.swagger.oas.models.media.UUIDSchema;
import org.apache.commons.lang3.StringUtils;

public class SchemaTypeUtil {

    private static final String TYPE = "type";
    private static final String FORMAT = "format";

    public static final String INTEGER_TYPE = "integer";
    public static final String NUMBER_TYPE = "number";
    public static final String STRING_TYPE = "string";
    public static final String BOOLEAN_TYPE = "boolean";
    public static final String OBJECT_TYPE = "object";

    public static final String INTEGER32_FORMAT = "int32";
    public static final String INTEGER64_FORMAT = "int64";
    public static final String FLOAT_FORMAT = "float";
    public static final String DOUBLE_FORMAT = "double";
    public static final String BYTE_FORMAT = "byte";
    public static final String BINARY_FORMAT = "binary";
    public static final String DATE_FORMAT = "date";
    public static final String DATE_TIME_FORMAT = "date-time";
    public static final String PASSWORD_FORMAT = "password";
    public static final String EMAIL_FORMAT = "email";
    public static final String UUID_FORMAT = "uuid";

    public static Schema createSchemaByType(ObjectNode node){
        if(node == null) {
            return new Schema();
        }
        final String type = getNodeValue(node, TYPE);
        if(StringUtils.isBlank(type)) {
            return new Schema();
        }
        final String format = getNodeValue(node, FORMAT);

        return createSchema(type, format);
    }

    public static Schema createSchema(String type, String format) {

        if(INTEGER_TYPE.equals(type)) {
            if(INTEGER64_FORMAT.equals(format)) {
                return new IntegerSchema().format(INTEGER64_FORMAT);
            }
            else {
                return new IntegerSchema();
            }
        }
        else if(NUMBER_TYPE.equals(type)) {
            if(FLOAT_FORMAT.equals(format)) {
                return new NumberSchema().format(FLOAT_FORMAT);
            }
            else if(DOUBLE_FORMAT.equals(format)) {
                return new NumberSchema().format(DOUBLE_FORMAT);
            }
            else {
                return new NumberSchema();
            }
        }
        else if(BOOLEAN_TYPE.equals(type)) {
            return new BooleanSchema();
        }
        else if(STRING_TYPE.equals(type)) {
            if(BYTE_FORMAT.equals(format)) {
                return new ByteArraySchema();
            }
            else if(BINARY_FORMAT.equals(format)) {
                return new BinarySchema();
            }
            else if(DATE_FORMAT.equals(format)) {
                return new DateSchema();
            }
            else if(DATE_TIME_FORMAT.equals(format)) {
                return new DateTimeSchema();
            }
            else if(PASSWORD_FORMAT.equals(format)) {
                return new PasswordSchema();
            }
            else if(EMAIL_FORMAT.equals(format)) {
                return new EmailSchema();
            }
            else if(UUID_FORMAT.equals(format)) {
                return new UUIDSchema();
            }
            else {
                return new StringSchema();
            }
        }
        else if(OBJECT_TYPE.equals(type)) {
            return new ObjectSchema();
        }
        else {
            return new Schema();
        }
    }

    private static String getNodeValue(ObjectNode node, String field) {
        final JsonNode jsonNode = node.get(field);
        if(jsonNode == null) {
            return null;
        }
        return jsonNode.textValue();
    }

}
