package io.swagger.transform.migrate;

public final class ApiDeclarationMigrator
        extends SwaggerJsonMigrator {
    public ApiDeclarationMigrator() {
        super(new V11ApiDeclarationMigrator());
    }

}
