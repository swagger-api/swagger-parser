package io.swagger.v3.parser.processors;


import io.swagger.v3.oas.models.OpenAPI;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.util.RefUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;
import static io.swagger.v3.parser.util.RefUtils.isAnExternalRefFormat;


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
            String oldName = RefUtils.computeDefinitionName(oldRef, new HashSet());
            String newName = RefUtils.computeDefinitionName(newRef, new HashSet());

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