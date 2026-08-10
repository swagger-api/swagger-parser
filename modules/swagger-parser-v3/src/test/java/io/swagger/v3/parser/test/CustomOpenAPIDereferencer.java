package io.swagger.v3.parser.test;

import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.reference.DereferencerContext;
import io.swagger.v3.parser.reference.OpenAPI31Traverser;
import io.swagger.v3.parser.reference.OpenAPIDereferencer31;
import io.swagger.v3.parser.reference.Reference;
import io.swagger.v3.parser.reference.ReferenceVisitor;
import io.swagger.v3.parser.reference.Traverser;
import io.swagger.v3.parser.reference.Visitor;
import io.swagger.v3.parser.urlresolver.PermittedUrlsChecker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class CustomOpenAPIDereferencer extends OpenAPIDereferencer31 {
    @Override
    public Visitor buildReferenceVisitor(DereferencerContext context, Reference reference, Traverser traverser) {
        return new CustomVisitor(reference, (OpenAPI31Traverser)traverser, new HashSet<>(), new HashMap<>());
    }

    @Override
    public ReferenceVisitor buildReferenceVisitorWithContext(DereferencerContext context, Reference reference, Traverser traverser) {
        return new CustomVisitor(reference, (OpenAPI31Traverser)traverser, new HashSet<>(), new HashMap<>(), context);
    }

    static public class CustomVisitor extends ReferenceVisitor {

        public CustomVisitor(Reference reference, OpenAPI31Traverser openAPITraverser, HashSet<Object> visited,
                             HashMap<Object, Object> visitedMap) {
            super(reference, openAPITraverser, visited, visitedMap);
        }
        public CustomVisitor(
                Reference reference,
                OpenAPI31Traverser openAPITraverser,
                HashSet<Object> visited,
                HashMap<Object, Object> visitedMap,
                DereferencerContext context) {
            super(reference, openAPITraverser, visited, visitedMap, context);
        }

        @Override
        public String readHttp(String uri, List<AuthorizationValue> auths, PermittedUrlsChecker permittedUrlsChecker) throws Exception {

            if (uri.startsWith("http://example.com/custom")) {
                return "openapi: 3.1.0\n" +
                        "info:\n" +
                        "  title: Domain\n" +
                        "  version: '1.0'\n" +
                        "components:\n" +
                        "  pathItems:\n" +
                        "    ExternalRef:\n" +
                        "      get:\n" +
                        "        description: ExternalRef domain\n" +
                        "        operationId: ExternalRef PathItem\n" +
                        "        responses:\n" +
                        "          '200':\n" +
                        "            description: OK";
            } else {
                return super.readHttp(uri, auths, permittedUrlsChecker);
            }
        }
    }
}
