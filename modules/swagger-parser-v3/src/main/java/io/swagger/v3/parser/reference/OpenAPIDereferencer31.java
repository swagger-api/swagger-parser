package io.swagger.v3.parser.reference;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class OpenAPIDereferencer31 implements OpenAPIDereferencer {

    protected final Set<String> messages = new HashSet<>();

    private OpenAPI openAPI;
    private SwaggerParseResult result;

    public boolean canDereference(DereferencerContext context) {
        if (context.openApi != null && context.openApi.getOpenapi().startsWith("3.1")) {
            return true;
        }
        return false;
    }

    public void dereference(DereferencerContext context, Iterator<OpenAPIDereferencer> chain) {

        // context.referenceCache = new ReferenceCache(context);

        if (!canDereference(context)) {
            if (chain.hasNext() && chain.next().canDereference(context)) {
                chain.next().dereference(context, chain);
                return;
            }
        }

        openAPI = context.openApi;
        result = context.swaggerParseResult;

        if (openAPI == null) {
            return;
        }

        LinkedHashMap<String, Reference> refSet = new LinkedHashMap<>();
        LinkedHashSet<String> msgs = new LinkedHashSet<>();
        if (StringUtils.isBlank(context.getRootUri())) {
            context.rootUri("local");
            context.currentUri("local");
        }
        if (context.getRootUri().equals("local")) {
            Reference localReference = new Reference()
                    .referenceSet(refSet)
                    .uri(context.getCurrentUri())
                    .messages(msgs)
                    .jsonNode(Json31.mapper().convertValue(openAPI, JsonNode.class))
                    .auths(context.getAuths());

            refSet.put("local", localReference);
        }

        Reference reference = new Reference()
                .referenceSet(refSet)
                .uri(context.getCurrentUri())
                .messages(msgs)
                .auths(context.getAuths());

        Traverser traverser = buildTraverser(context);
        ReferenceVisitor referenceVisitor = buildReferenceVisitorWithContext(context, reference, traverser);
        try {
            openAPI = traverser.traverse(context.getOpenApi(), referenceVisitor);
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (openAPI == null) {
            return;
        }

        result.setOpenAPI(openAPI);
        result.getMessages().addAll(reference.getMessages());
    }

    public Traverser buildTraverser(DereferencerContext context) {
        return new OpenAPI31Traverser(context);
    }

    public Visitor buildReferenceVisitor(DereferencerContext context, Reference reference, Traverser traverser) {
        return new ReferenceVisitor(reference, (OpenAPI31Traverser)traverser, new HashSet<>(), new HashMap<>());
    }

    public ReferenceVisitor buildReferenceVisitorWithContext(DereferencerContext context, Reference reference, Traverser traverser) {
        return new ReferenceVisitor(reference, (OpenAPI31Traverser)traverser, new HashSet<>(), new HashMap<>(), context);
    }

}
