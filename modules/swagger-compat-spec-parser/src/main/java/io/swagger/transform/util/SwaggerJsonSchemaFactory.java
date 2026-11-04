package io.swagger.transform.util;

import com.networknt.schema.ExecutionContext;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.format.Format;

import java.net.URI;
import java.net.URISyntaxException;

public final class SwaggerJsonSchemaFactory {
    private static final String BASE_URI
            = "http://swagger-api.github.io/schemas/";
    private static final String ROOT_CONTEXT = "classpath:schemas/";

    private static final SwaggerJsonSchemaFactory INSTANCE
            = new SwaggerJsonSchemaFactory();

    private final SchemaRegistry registry;

    private SwaggerJsonSchemaFactory() {
        final Dialect dialect = Dialect.builder(Dialects.getDraft4())
                .format(new UriReferenceFormat())
                .build();
        registry = SchemaRegistry.withDefaultDialect(dialect,
                builder -> builder.schemaIdResolvers(
                        resolvers -> resolvers.mapPrefix(BASE_URI, ROOT_CONTEXT)));
    }

    public static SwaggerJsonSchemaFactory getInstance() {
        return INSTANCE;
    }

    public SchemaRegistry getRegistry() {
        return registry;
    }

    /**
     * Load one of the bundled Swagger schemas
     *
     * @param schemaPath path relative to the schemas root, for instance
     *                   {@code v1.2/apiDeclaration.json}
     * @return the schema
     */
    public Schema getSchema(final String schemaPath) {
        return registry.getSchema(SchemaLocation.of(BASE_URI + schemaPath));
    }

    /**
     * {@code uri} format accepting URI references
     *
     * <p>Swagger 1.x documents use relative URIs (for instance {@code "/pet"})
     * in members declared with {@code "format": "uri"}. The draft 4 format
     * requires absolute URIs, so it is replaced by a check that the value is a
     * syntactically valid URI reference.</p>
     */
    private static final class UriReferenceFormat implements Format {
        @Override
        public String getName() {
            return "uri";
        }

        @Override
        public boolean matches(final ExecutionContext executionContext,
                               final String value) {
            try {
                new URI(value);
                return true;
            } catch (URISyntaxException e) {
                return false;
            }
        }
    }
}
