package io.swagger.transform.migrate;

public final class ResourceListingMigrator
        extends SwaggerJsonMigrator {
    public ResourceListingMigrator() {
        super(new V11ResourceListingMigrator());
    }
}
