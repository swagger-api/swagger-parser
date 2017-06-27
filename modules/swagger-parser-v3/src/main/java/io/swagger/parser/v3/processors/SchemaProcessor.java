package io.swagger.parser.v3.processors;


import io.swagger.oas.models.media.AllOfSchema;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.Schema;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;

import java.util.List;
import java.util.Map;

import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;


public class SchemaProcessor {
    private final ResolverCache cache;


    public SchemaProcessor(ResolverCache cache) {
        this.cache = cache;
    }


    public Schema processSchema(Schema schema) {
        if (schema != null) {
            if (schema.get$ref() != null) {
                return processReferenceSchema(schema);
            } else {
                processSchemaType(schema);
            }
        }
        return schema;
    }

    public void processSchemaType(Schema schema){

        if (schema instanceof ArraySchema) {
            processArraySchema((ArraySchema) schema);
        } else if (schema instanceof AllOfSchema) {
            processAllOfSchema((AllOfSchema) schema);
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
        Schema resolved;
        if (schema.getAdditionalProperties() != null){
            if(schema.getAdditionalProperties().get$ref() != null){
                resolved = processReferenceSchema(schema.getAdditionalProperties());
                schema.setAdditionalProperties(resolved);
            }else{
                processSchemaType(schema.getAdditionalProperties());
            }
        }
    }

    private void processNotSchema(Schema schema) {
        Schema resolved;
        if (schema.getNot() != null){
            if(schema.getNot().get$ref() != null){
                resolved = processReferenceSchema(schema.getNot());
                schema.setNot(resolved);
            }else{
                processSchemaType(schema.getNot());
            }
        }
    }

    public void processPropertySchema(Schema schema) {
        if(schema.get$ref() !=  null){
            processReferenceSchema(schema);
        }

        Schema resolved = null;
        String propertyName = null;
         Map<String, Schema> properties = schema.getProperties();
         if (properties != null) {
             for (Map.Entry<String, Schema> propertyEntry : properties.entrySet()) {
                 Schema property = propertyEntry.getValue();
                 if (property instanceof ArraySchema) {
                     processArraySchema((ArraySchema) property);
                 }
                 if(property.get$ref() != null) {
                     propertyName = propertyEntry.getKey();
                     resolved = processReferenceSchema(property);
                     properties.replace(propertyName,resolved);

                 }
             }
         }
    }

    public void processAllOfSchema(AllOfSchema allOfSchema) {

        final List<Schema> schemas = allOfSchema.getAllOf();
        if (schemas != null) {
            for (Schema schema : schemas) {
                Schema resolved = null;
                if (schema.get$ref() != null) {
                    resolved = processReferenceSchema(schema);
                    schemas.add(resolved);
                }else{
                    processSchemaType(schema);
                }
            }
        }

    }

    public void processArraySchema(ArraySchema arraySchema) {

        final Schema items = arraySchema.getItems();
        Schema resolved = null;
        if (items.get$ref() != null) {
            resolved = processReferenceSchema(items);
            arraySchema.setItems(resolved);
        }else{
            processSchemaType(items);
        }
    }

    public Schema processReferenceSchema(Schema schema){
        RefFormat refFormat = computeRefFormat(schema.get$ref());
        String $ref = schema.get$ref();
        Schema newSchema = cache.loadRef($ref, refFormat, Schema.class);
        return newSchema;
    }
}