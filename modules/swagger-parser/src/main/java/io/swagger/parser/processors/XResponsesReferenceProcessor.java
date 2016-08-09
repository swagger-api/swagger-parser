package io.swagger.parser.processors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import io.swagger.models.RefResponse;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.refs.GenericRef;
import io.swagger.models.refs.RefFormat;
import io.swagger.models.refs.RefType;
import io.swagger.parser.ResolverCache;
import io.swagger.parser.util.DeserializationUtils;

import java.util.Map;

/**
 *
 */
public class XResponsesReferenceProcessor {

	private final ResolverCache cache;
	private final Swagger swagger;
	private final ExternalRefProcessor externalRefProcessor;

	public XResponsesReferenceProcessor(ResolverCache cache, Swagger swagger) {
		this.cache = cache;
		this.swagger = swagger;
		this.externalRefProcessor = new ExternalRefProcessor(cache, swagger);
	}

	public void processXResponsesReferences() {
		Map<String, Object> vendorExtensions = swagger.getVendorExtensions();
		if (vendorExtensions != null && vendorExtensions.containsKey("x-responses-reference")) {
			ObjectNode xResponsesReference = (ObjectNode) vendorExtensions.get("x-responses-reference");

			if (xResponsesReference != null && !xResponsesReference.isNull()) {
				String $ref = xResponsesReference.get("$ref").asText();
				GenericRef ref = new GenericRef(RefType.RESPONSE, $ref);
				String fileName = $ref.split("#/")[0];

				Map<String, Object> responses = cache.loadRef($ref, ref.getFormat(), Map.class);

				Map<String, Response> resp = Maps.newHashMap();
				for (Map.Entry<String, Object> entry : responses.entrySet()) {
					Response response = DeserializationUtils.deserialize(entry.getValue(), "", Response.class);
					RefProperty refProperty = (RefProperty) response.getSchema();
					resp.put(entry.getKey(), response);

					externalRefProcessor.processRefToExternalDefinition(fileName + refProperty.get$ref(), RefFormat.RELATIVE);
				}
				swagger.setResponses(resp);
			}
		}
	}
}
