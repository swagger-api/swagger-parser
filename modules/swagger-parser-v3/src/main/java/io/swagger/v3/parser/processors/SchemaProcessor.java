package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.OpenAPI;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.extensions.JsonSchemaParserExtension;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.models.RefType;
import io.swagger.v3.parser.util.OpenAPIDeserializer;
import io.swagger.v3.parser.util.RefUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;
import static io.swagger.v3.parser.util.RefUtils.isAnExternalRefFormat;


public class SchemaProcessor {
    private final ExternalRefProcessor externalRefProcessor;
    private boolean openapi31;
    private final ResolverCache cache;
    private OpenAPI openAPI;


    public SchemaProcessor(ResolverCache cache, OpenAPI openAPI) {
        this(cache,openAPI, false);
    }

    public SchemaProcessor(ResolverCache cache, OpenAPI openAPI, boolean openapi31) {
        this.openapi31 = openapi31;
        this.cache = cache;
        this.openAPI = openAPI;
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }


    public void processSchema(Schema schema) {

        if (schema == null) {
            return;
        }
        if (openapi31) {
            // TODO use as singleton somewhere loaded as static used by both here and deserializer
            List<JsonSchemaParserExtension> jsonschemaExtensions = OpenAPIDeserializer.getJsonSchemaParserExtensions();
            for (JsonSchemaParserExtension jsonschemaExtension: jsonschemaExtensions) {
                if (jsonschemaExtension.resolveSchema(schema, cache, openAPI, openapi31)) {
                    return;
                }
            }
        }

        if (schema.get$ref() != null) {
            processReferenceSchema(schema);
        } else {
            processSchemaType(schema);
        }

    }

    public void processSchemaType(Schema schema){

        if (schema instanceof ArraySchema) {
            processArraySchema((ArraySchema) schema);
        }
        if (schema instanceof ComposedSchema) {
            processComposedSchema((ComposedSchema) schema);
        }

        if(schema.getProperties() != null && schema.getProperties().size() > 0){
            processPropertySchema(schema);
        }

        if(schema.getNot() != null){
            processNotSchema(schema);
        }
        if(schema.getAdditionalProperties() != null){
            processAdditionalProperties(schema);

        }
        if (schema.getDiscriminator() != null) {
            processDiscriminatorSchema(schema);
        }

    }

    private void processDiscriminatorSchema(Schema schema) {
        if (schema.getDiscriminator() != null && schema.getDiscriminator().getMapping() != null) {
            Map<String, String> mapping = schema.getDiscriminator().getMapping();
            for (String ref : mapping.values()) {
                processReferenceSchema(new Schema().$ref(ref));
            }
        }
    }

    private void processAdditionalProperties(Object additionalProperties) {

        if (additionalProperties instanceof Schema) {
            Schema schema = (Schema) additionalProperties;
            if (schema.getAdditionalProperties() != null && schema.getAdditionalProperties() instanceof Schema) {
                Schema additionalPropertiesSchema = (Schema) schema.getAdditionalProperties();
                if (additionalPropertiesSchema.get$ref() != null) {
                    processReferenceSchema(additionalPropertiesSchema);
                } else {
                    processSchemaType(additionalPropertiesSchema);
                }
            }
        }
    }

    private void processNotSchema(Schema schema) {

        if (schema.getNot() != null){
            if(schema.getNot().get$ref() != null){
                processReferenceSchema(schema.getNot());
            }else{
                processSchemaType(schema.getNot());
            }
        }
    }

    public void processPropertySchema(Schema schema) {
        if(schema.get$ref() !=  null){
            processReferenceSchema(schema);
        }

        Map<String, Schema> properties = schema.getProperties();
        if (properties != null) {
            for (Map.Entry<String, Schema> propertyEntry : properties.entrySet()) {
                Schema property = propertyEntry.getValue();
                if(property.get$ref() != null) {
                    processReferenceSchema(property);
                }else {
                    processSchemaType(property);
                }
            }
        }
    }

    public void processComposedSchema(ComposedSchema composedSchema) {
        if(composedSchema.getAllOf() != null) {
            final List<Schema> schemas = composedSchema.getAllOf();
            if (schemas != null) {
                for (Schema schema : schemas) {
                    if (schema.get$ref() != null) {
                        processReferenceSchema(schema);
                    } else {
                        processSchemaType(schema);
                    }
                }
            }
        }if(composedSchema.getOneOf() != null){
            final List<Schema> schemas = composedSchema.getOneOf();
            if (schemas != null) {
                for (Schema schema : schemas) {
                    if (schema.get$ref() != null) {
                        String oldRef = schema.get$ref();
                        processReferenceSchema(schema);
                        String newRef = schema.get$ref();
                        changeDiscriminatorMapping(composedSchema, oldRef, newRef);
                    } else {
                        processSchemaType(schema);
                    }
                }
            }
        }if(composedSchema.getAnyOf() != null){
            final List<Schema> schemas = composedSchema.getAnyOf();
            if (schemas != null) {
                for (Schema schema : schemas) {
                    if (schema.get$ref() != null) {
                        processReferenceSchema(schema);
                    } else {
                        processSchemaType(schema);
                    }
                }
            }
        }

    }

    private void changeDiscriminatorMapping(ComposedSchema composedSchema, String oldRef, String newRef) {
        Discriminator discriminator = composedSchema.getDiscriminator();
        if (!oldRef.equals(newRef) && discriminator != null) {
            String oldName = RefUtils.computeDefinitionName(oldRef);
            String newName = RefUtils.computeDefinitionName(newRef);

            String mappingName = null;
            if (discriminator.getMapping() != null) {
                for (String name : discriminator.getMapping().keySet()) {
                    if (oldRef.equals(discriminator.getMapping().get(name))) {
                        mappingName = name;
                        break;
                    }
                }
                if (mappingName != null) {
                    discriminator.getMapping().put(mappingName, newRef);
                }
            }

            if (mappingName == null && !oldName.equals(newName)) {
                if (discriminator.getMapping() == null) {
                    discriminator.setMapping(new HashMap());
                }
                discriminator.getMapping().put(oldName, newRef);
            }

        }
    }

    public void processArraySchema(ArraySchema arraySchema) {

        final Schema items = arraySchema.getItems();
        if (items.get$ref() != null) {
            processReferenceSchema(items);
        }else{
            processSchemaType(items);
        }
    }

    private void processReferenceSchema(Schema schema) {
    /* if this is a URL or relative ref:
        1) we need to load it into memory.
        2) shove it into the #/components/schemas
        3) update the RefModel to point to its location in #/components/schemas
     */
        RefFormat refFormat = computeRefFormat(schema.get$ref());
        String $ref = schema.get$ref();

        if (isAnExternalRefFormat(refFormat)){
            final String newRef = externalRefProcessor.processRefToExternalSchema($ref, refFormat);

            if (newRef != null) {
                schema.set$ref(RefType.SCHEMAS.getInternalPrefix() + newRef);
            }
        }
    }

}