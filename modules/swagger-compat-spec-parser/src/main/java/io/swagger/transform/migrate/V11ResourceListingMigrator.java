package io.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.net.InternetDomainName;
import io.swagger.transform.migrate.resourcelisting.PathAppenderMigrator;
import io.swagger.transform.util.MutableJsonTree;
import io.swagger.transform.util.SwaggerMigrationException;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Objects;
import java.util.regex.Pattern;

import static io.swagger.transform.util.SwaggerMigrators.membersToString;
import static io.swagger.transform.util.SwaggerMigrators.patchFromResource;

/**
 * Migrator for a complete 1.1 resource listing
 */
public final class V11ResourceListingMigrator
        implements SwaggerMigrator {
    /*
     * Schemes are case insensitive
     */
    private static final Pattern LEGAL_SCHEMES
            = Pattern.compile("https?", Pattern.CASE_INSENSITIVE);

    /**
     * Check the validity of a "basePath" argument at the root of a resource
     * listing
     *
     * <p>A {@code basePath} is valid if it obeys the following conditions:</p>
     *
     * <ul>
     * <li>it is an absolute URI;</li>
     * <li>its scheme is {@code http} or {@code https};</li>
     * <li>it has a hostname (meh);</li>
     * <li>its path component must not end with a {@code /}</li>
     * </ul>
     *
     * @param input the input (never null)
     * @return the input, as a normalized URI (as defined by RFC 3986)
     * @throws IllegalArgumentException invalid base path, see description
     */
    private static String checkLegalBasePath(@Nonnull final String input) {
        // Fails with IllegalArgumentException if URI is not valid...
        final URI uri = URI.create(input).normalize();

        // And so does Preconditions.checkArgument()
        Preconditions.checkArgument(uri.isAbsolute(), "basePath has no scheme");

        String tmp;
        // scheme cannot be null since URI is absolute
        tmp = uri.getScheme();
        Preconditions.checkArgument(LEGAL_SCHEMES.matcher(tmp).matches(),
                "basePath has scheme " + tmp + ", expected either http or https");

        // hostname, however, may be...
        tmp = Strings.nullToEmpty(uri.getHost());
        Preconditions.checkArgument(InternetDomainName.isValid(tmp),
                "basePath has an invalid hostname " + tmp);

        // ... Same for path...
        tmp = Strings.nullToEmpty(uri.getPath());
        // ... Which must not end with a /
        Preconditions.checkArgument(!tmp.endsWith("/"),
                "basePath's path component " + tmp + " must not end with a /");

        return uri.toString();
    }

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
            throws SwaggerMigrationException {
        Objects.requireNonNull(input);
        final MutableJsonTree tree = new MutableJsonTree(input);

        SwaggerMigrator migrator;

        migrator = membersToString("swaggerVersion", "apiVersion");
        tree.applyMigrator(migrator);

        migrator = patchFromResource("/patches/v1.1/versionChange.json");
        tree.applyMigrator(migrator);

        /*
         * Issue #4: we need to check for the presence of "basePath"; if it is
         * a JSON String, check that it is an absolute URI with protocol HTTP,
         * and a path component not ending with "/".
         *
         * If all of this is true, instantiate a PathAppenderMigrator and apply
         * it over all of /apis.
         */
        if (input.path("basePath").isTextual()) {
            String basePath = input.get("basePath").textValue();
            ((ObjectNode) tree.getCurrentNode()).remove("basePath");

            try {
                basePath = checkLegalBasePath(basePath);
            } catch (IllegalArgumentException e) {
                throw new SwaggerMigrationException(e.getMessage());
            }

            migrator = new PathAppenderMigrator(basePath);
            tree.setPointer(JsonPointer.of("apis"));
            tree.applyMigratorToElements(migrator);
        }

        return tree.getBaseNode();
    }
}
