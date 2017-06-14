package io.swagger.parser.v3.processors;


import io.swagger.oas.models.media.AllOfSchema;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.media.Schema;
import io.swagger.parser.ResolverCache;


import java.util.List;
import java.util.Map;

//import static io.swagger.parser.util.RefUtils.isAnExternalRefFormat;


public class SchemaProcessor {
    //private final PropertyProcessor propertyProcessor;
    //private final ExternalRefProcessor externalRefProcessor;

    public SchemaProcessor(ResolverCache cache, OpenAPI openApi) {
       // this.propertyProcessor = new PropertyProcessor(cache, openApi);
        //this.externalRefProcessor = new ExternalRefProcessor(cache, openApi);
    }

    public void processSchemaType(Schema schema) {
        if (schema == null) {
            return;
        }

        /*if (schema instanceof RefModel) {
            processRefModel((RefModel) model);
        } else */ if (schema instanceof ArraySchema) {
            processArraySchema((ArraySchema) schema);
        } else if (schema instanceof AllOfSchema) {
            processAllOfSchema((AllOfSchema) schema);
        } else if (schema instanceof AllOfSchema) {
            processSchema(schema);
        }
    }

    /*private*/ void processSchema(Schema schema) {

        final Map<String, Schema> properties = schema.getProperties();

        if (properties == null) {
            return;
        }

        for (Map.Entry<String, Schema> propertyEntry : properties.entrySet()) {
            final Schema property = propertyEntry.getValue();
           // propertyProcessor.processProperty(property);
        }

    }

    /*private*/ void processAllOfSchema(AllOfSchema allOfSchema) {

        processSchemaType(allOfSchema);


        /*final List<RefModel> interfaces = AllOfSchema.getInterfaces();
        if (interfaces != null) {
            for (RefModel model : interfaces) {
                processRefModel(model);
            }
        }*/

    }

    /*private*/ void processArraySchema(ArraySchema arraySchema) {

        final Schema items = arraySchema.getItems();

        // ArrayModel has a properties map, but my reading of the swagger spec makes me think it should be ignored

        if (items != null) {
            //propertyProcessor.processProperty(items);
        }
    }


    /*private void processRefModel(RefModel refModel) {
    /* if this is a URL or relative ref:
        1) we need to load it into memory.
        2) shove it into the #/definitions
        3) update the RefModel to point to its location in #/definitions
     */

        /*if (isAnExternalRefFormat(refModel.getRefFormat())) {
            final String newRef = externalRefProcessor.processRefToExternalDefinition(refModel.get$ref(), refModel.getRefFormat());

            if (newRef != null) {
                refModel.set$ref(newRef);
            }
        }
    }*/


}