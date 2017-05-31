package io.swagger.parser.processors;

import static io.swagger.parser.util.RefUtils.isAnExternalRefFormat;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.models.Model;
import io.swagger.models.refs.RefFormat;
import io.swagger.parser.ResolverCache;

public class VendorExtensionProcessor {
	
	private final ExternalRefProcessor externalRefProcessor;
	private final ResolverCache cache;
	
	public VendorExtensionProcessor(ResolverCache cache, ExternalRefProcessor externalRefProcessor) {
		this.externalRefProcessor = externalRefProcessor;
		this.cache = cache;
	}

    // Copied from GenericRef::computeRefFormat
    private static RefFormat computeRefFormat(String ref) {
        RefFormat result = RefFormat.INTERNAL;
        if (ref.startsWith("http")) {
            result = RefFormat.URL;
        } else if (ref.startsWith("#/")) {
            result = RefFormat.INTERNAL;
        } else if (ref.startsWith(".") || ref.startsWith("/")) {
            result = RefFormat.RELATIVE;
        }

        return result;
    }

    private void processRefsRecursively(Object node, String externalFile) {
	    if (node != null && node instanceof ObjectNode) {
	        ObjectNode objectNode = (ObjectNode) node;
            if (objectNode.has("$ref")) {
                String ref = objectNode.get("$ref").asText();
                RefFormat refFormat = computeRefFormat(ref);
                if (isAnExternalRefFormat(refFormat)) {
                    Pattern pattern = Pattern.compile("(#/[^/]+/)");
                    Matcher matcher = pattern.matcher(ref);
                    if (!matcher.find()) {
                        throw new RuntimeException("Unable to infer ref type: " + ref);
                    }
                    String refType = matcher.group(0);
                    objectNode.put("$ref", refType + externalRefProcessor.processRefToExternalDefinition(ref, refFormat));
                } else if (externalFile != null) {
                    externalRefProcessor.processRefToExternalDefinition(externalFile + ref, RefFormat.RELATIVE);
                }
            } else {
                Iterator<JsonNode> it = objectNode.elements();
                while (it.hasNext()) {
                    processRefsRecursively(it.next(), externalFile);
                }
            }
        }
    }

	public void processRefsFromVendorExtensions(Model model, String externalFile) {
        Map<String, Object> vendorExtensions = model.getVendorExtensions();
        if (vendorExtensions != null) {
            for (Map.Entry<String, Object> entry : vendorExtensions.entrySet()) {
                processRefsRecursively(entry.getValue(), externalFile);
            }
        }
    }

}

