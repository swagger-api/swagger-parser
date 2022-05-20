package io.swagger.v3.parser.reference;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

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

        // Set<Object> visitedSet = new HashSet<>();
        Reference reference = new Reference()
                .referenceSet(new LinkedHashMap<>())
                .uri(context.getCurrentUri())
                .messages(new LinkedHashSet<>())
                // .visitedSet(visitedSet)
                .auths(context.getAuths());

        Traverser traverser = buildTraverser(context);
        Visitor referenceVisitor = buildReferenceVisitor(context, reference, traverser);
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
}
