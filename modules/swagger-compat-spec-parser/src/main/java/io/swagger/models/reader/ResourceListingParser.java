package io.swagger.models.reader;

import io.swagger.models.resourcelisting.ApiListingReference;
import io.swagger.models.resourcelisting.Authorization;
import io.swagger.models.resourcelisting.GrantTypes;
import io.swagger.models.resourcelisting.ResourceListing;
import io.swagger.report.MessageBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceListingParser extends SwaggerParser {

    public ResourceListingParser() {
        super();
    }

    public ResourceListing read(String json, MessageBuilder messages) {
        return null;
    }

    private Map<String, Authorization> parseAuthorizations(Map<String, Object> authorizations, MessageBuilder messages) {
        return new HashMap<>();
    }

    private List<GrantTypes> readGrantTypes(Map<String, Object> map, MessageBuilder messages) {
        return new ArrayList<GrantTypes>();
    }

    private ApiListingReference readApiListingReference(Map<String, Object> map, MessageBuilder messages) {
        return null;
    }
}