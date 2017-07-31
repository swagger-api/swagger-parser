package io.swagger.parser.v3.processors;


import io.swagger.oas.models.OpenAPI;

import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.ComposedSchema;
import io.swagger.oas.models.media.Schema;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;


import java.util.List;
import java.util.Map;

import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;
import static io.swagger.parser.v3.util.RefUtils.isAnExternalRefFormat;


public class SchemaProcessor {
    private final ResolverCache cache;
    private final ExternalRefProcessor externalRefProcessor;


    public SchemaProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }


    public void processSchema(Schema schema) {
        if (schema != null) {
            if (schema.get$ref() != null) {
                processReferenceSchema(schema);
            } else {
                processSchemaType(schema);
            }
        }
    }

    public void processSchemaType(Schema schema){

        if (schema instanceof ArraySchema) {
            processArraySchema((ArraySchema) schema);
        } else if (schema instanceof ComposedSchema) {
            processComposedSchema((ComposedSchema) schema);
        }

        if(schema.getProperties()!= null){
            processPropertySchema(schema);
        }
        if(schema.getNot() != null){
            processNotSchema(schema);
        }
        if(schema.getAdditionalProperties() != null){
            processAdditionalProperties(schema);
            
        }

    }

    private void processAdditionalProperties(Schema schema) {

        if (schema.getAdditionalProperties() != null){
            if(schema.getAdditionalProperties().get$ref() != null){
                processReferenceSchema(schema.getAdditionalProperties());
            }else{
                processSchemaType(schema.getAdditionalProperties());
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
                 if (property instanceof ArraySchema) {
                     processArraySchema((ArraySchema) property);
                 }
                 if(property.get$ref() != null) {
                     processReferenceSchema(property);
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
                        processReferenceSchema(schema);
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

    public void processArraySchema(ArraySchema arraySchema) {

        final Schema items = arraySchema.getItems();
        if (items.get$ref() != null) {
            processReferenceSchema(items);
        }else{
            processSchemaType(items);
        }
    }

   /* public Schema processReferenceSchema(Schema schema){
        RefFormat refFormat = computeRefFormat(schema.get$ref());
        String $ref = schema.get$ref();
        Schema newSchema = cache.loadRef($ref, refFormat, Schema.class);
        return newSchema;
    }*/

    private void processReferenceSchema(Schema schema) {
    /* if this is a URL or relative ref:
        1) we need to load it into memory.
        2) shove it into the #/definitions
        3) update the RefModel to point to its location in #/definitions
     */
        RefFormat refFormat = computeRefFormat(schema.get$ref());
        String $ref = schema.get$ref();

        if (isAnExternalRefFormat(refFormat)){
            final String newRef = externalRefProcessor.processRefToExternalSchema($ref, refFormat);

            if (newRef != null) {
                schema.set$ref(newRef);
            }
        }
    }

}