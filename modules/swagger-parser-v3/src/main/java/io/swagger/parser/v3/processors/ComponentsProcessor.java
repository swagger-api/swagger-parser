package io.swagger.parser.v3.processors;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.callbacks.Callback;
import io.swagger.oas.models.examples.Example;
import io.swagger.oas.models.headers.Header;
import io.swagger.oas.models.links.Link;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.security.SecurityScheme;
import io.swagger.parser.v3.ResolverCache;
import jdk.nashorn.internal.codegen.CompilerConstants;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


/**
 * Created by gracekarina on 13/06/17.
 */
public class ComponentsProcessor {
    private final ResolverCache cache;
    private final OpenAPI openApi;
    private final SchemaProcessor schemaProcessor;
    private final ResponseProcessor responseProcessor;
    private final RequestBodyProcessor requestBodyProcessor;
    private final ParameterProcessor parameterProcessor;
    private final HeaderProcessor headerProcessor;
    private final ExampleProcessor exampleProcessor;
    private final LinkProcessor linkProcessor;
    private final CallbackProcessor callbackProcessor;
    private final SecuritySchemeProcessor securitySchemeProcessor;

    public ComponentsProcessor(OpenAPI openApi,ResolverCache cache){
        this.cache = cache;
        this.openApi = openApi;
        this.schemaProcessor = new SchemaProcessor(cache);
        this.responseProcessor = new ResponseProcessor(cache, openApi);
        this.requestBodyProcessor = new RequestBodyProcessor(cache, openApi);
        this.parameterProcessor = new ParameterProcessor(cache, openApi);
        this.headerProcessor = new HeaderProcessor(cache, openApi);
        this.exampleProcessor = new ExampleProcessor(cache,openApi);
        this.linkProcessor = new LinkProcessor(cache,openApi);
        this.callbackProcessor = new CallbackProcessor(cache,openApi);
        this.securitySchemeProcessor = new SecuritySchemeProcessor(cache,openApi);

    }


    public void processComponents() {
        final Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        final Map<String, ApiResponse> responses = openApi.getComponents().getResponses();
        final Map<String, RequestBody> requestBodies = openApi.getComponents().getRequestBodies();
        final Map<String, Parameter> parameters = openApi.getComponents().getParameters();
        final Map<String, Header> headers = openApi.getComponents().getHeaders();
        final Map<String, Example> examples = openApi.getComponents().getExamples();
        final Map<String, Link> links = openApi.getComponents().getLinks();
        final Map<String, Callback> callbacks = openApi.getComponents().getCallbacks();
        final Map<String, SecurityScheme> securitySchemes = openApi.getComponents().getSecuritySchemes();

        //schemas
        if (schemas != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while(schemas.keySet().size() > keySet.size()) {
                processSchemas(keySet, schemas);
            }
        }

        //responses
        if (responses != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while(responses.keySet().size() > keySet.size()) {
                processResponses(keySet, responses);
            }
        }

        //requestBodies
        if (requestBodies != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while(requestBodies.keySet().size() > keySet.size()) {
                processRequestBodies(keySet, requestBodies);
            }
        }

        //parameters
        if (parameters != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while(parameters.keySet().size() > keySet.size()) {
                processParameters(keySet, parameters);
            }
        }

        //headers
        if (headers != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while(headers.keySet().size() > keySet.size()) {
                processHeaders(keySet, headers);
            }
        }

        //examples
        if (examples != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while(examples.keySet().size() > keySet.size()) {
                processExamples(keySet, examples);
            }
        }

        //links
        if (links != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while(links.keySet().size() > keySet.size()) {
                processLinks(keySet, links);
            }
        }

        //callbacks
        if (callbacks != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while(callbacks.keySet().size() > keySet.size()) {
                processCallbacks(keySet, callbacks);
            }
        }

        //securitySchemes
        if (securitySchemes != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while(securitySchemes.keySet().size() > keySet.size()) {
                processSecuritySchemes(keySet, securitySchemes);
            }
        }
    }

    private void processSecuritySchemes(Set<String> securitySchemeKey, Map<String, SecurityScheme> securitySchemes) {
        securitySchemeKey.addAll(securitySchemes.keySet());

        for (String securitySchemeName : securitySchemeKey) {
            final SecurityScheme securityScheme = securitySchemes.get(securitySchemeName);
            SecurityScheme resolvedSecurityScheme = securitySchemeProcessor.processSecurityScheme(securityScheme);
            securitySchemes.replace(securitySchemeName,securityScheme,resolvedSecurityScheme);
        }
    }

    private void processCallbacks(Set<String> callbackKey, Map<String, Callback> callbacks) {
        callbackKey.addAll(callbacks.keySet());

        for (String callbackName : callbackKey) {
            final Callback callback = callbacks.get(callbackName);
            Callback resolvedCallback = callbackProcessor.processCallback(callback);
            callbacks.replace(callbackName, callback, resolvedCallback);
        }
    }

    private void processLinks(Set<String> linkKey, Map<String, Link> links) {
        linkKey.addAll(links.keySet());

        for (String linkName : linkKey) {
            final Link link = links.get(linkName);
            Link resolvedLink = linkProcessor.processLink(link);
            links.replace(linkName,link,resolvedLink);
        }

    }

    private void processExamples(Set<String> exampleKey, Map<String, Example> examples) {
        exampleKey.addAll(examples.keySet());

        for (String exampleName : exampleKey) {
            final Example example = examples.get(exampleName);
            Example resolvedExample = exampleProcessor.processExample(example);
            examples.replace(exampleName,example,resolvedExample);
        }
    }

    private void processHeaders(Set<String> HeaderKey, Map<String, Header> headers) {
        HeaderKey.addAll(headers.keySet());

        for (String headersName : HeaderKey) {
            final Header header = headers.get(headersName);
            Header resolvedHeader = headerProcessor.processHeader(header);
            headers.replace(headersName,header,resolvedHeader);
        }
    }

    private void processParameters(Set<String> ParametersKey, Map<String, Parameter> parameters) {
        ParametersKey.addAll(parameters.keySet());

        for (String parametersName : ParametersKey) {
            final Parameter parameter = parameters.get(parametersName);
            Parameter resolvedParameter  = parameterProcessor.processParameter(parameter);
            parameters.replace(parametersName,parameter,resolvedParameter);
        }
    }

    private void processRequestBodies(Set<String> requestBodyKey, Map<String, RequestBody> requestBodies) {
        requestBodyKey.addAll(requestBodies.keySet());

        for (String requestBodyName : requestBodyKey) {
            final RequestBody requestBody = requestBodies.get(requestBodyName);
            RequestBody resolvedBody = requestBodyProcessor.processRequestBody(requestBody);
            requestBodies.replace(requestBodyName,requestBody,resolvedBody);
        }
    }

    private void processResponses(Set<String> responseKey, Map<String, ApiResponse> responses) {
        responseKey.addAll(responses.keySet());

        for (String responseName : responseKey) {
            final ApiResponse response = responses.get(responseName);
            ApiResponse resolvedResponse = responseProcessor.processResponse(response);
            responses.replace(responseName,response,resolvedResponse);
        }
    }

    public void processSchemas(Set<String> schemaKeys, Map<String, Schema> schemas) {
        schemaKeys.addAll(schemas.keySet());

        for (String schemaName : schemaKeys) {
            final Schema schema = schemas.get(schemaName);
            schemaProcessor.processSchema(schema);
        }
    }
}
