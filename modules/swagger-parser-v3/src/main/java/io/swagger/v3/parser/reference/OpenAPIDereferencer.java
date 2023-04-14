package io.swagger.v3.parser.reference;

import io.swagger.v3.parser.urlresolver.exceptions.HostDeniedException;

import java.util.Iterator;

public interface OpenAPIDereferencer {

    boolean canDereference(DereferencerContext context);
    void dereference(DereferencerContext context, Iterator<OpenAPIDereferencer> chain) throws HostDeniedException;

    Traverser buildTraverser(DereferencerContext context);

    public Visitor buildReferenceVisitor(DereferencerContext context, Reference reference, Traverser traverser);

}
