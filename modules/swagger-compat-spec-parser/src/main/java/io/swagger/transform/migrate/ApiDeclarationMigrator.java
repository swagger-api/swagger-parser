package io.swagger.transform.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.report.Message;
import io.swagger.report.MessageBuilder;

public final class ApiDeclarationMigrator
    extends SwaggerJsonMigrator
{
    public ApiDeclarationMigrator()
    {
        super(new V11ApiDeclarationMigrator());
    }

}
