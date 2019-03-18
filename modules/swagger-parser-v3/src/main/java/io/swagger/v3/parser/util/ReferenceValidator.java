package io.swagger.v3.parser.util;

import io.swagger.v3.oas.models.Components;


public enum ReferenceValidator {

    schemas {
        @Override
        public boolean validateComponent(Components components,String reference) {
            return components.getSchemas().containsKey(reference);
        }
    },
    responses {
        @Override
        public boolean validateComponent(Components components,String reference) {
            return components.getResponses().containsKey(reference);
        }
    },
    parameters {
        @Override
        public boolean validateComponent(Components components,String reference) {
            return components.getParameters().containsKey(reference);
        }
    },
    examples {
        @Override
        public boolean validateComponent(Components components,String reference) {
            return components.getExamples().containsKey(reference);
        }
    },
    requestBodies {
        @Override
        public boolean validateComponent(Components components,String reference) {
            return components.getRequestBodies().containsKey(reference);
        }
    },
    headers {
        @Override
        public boolean validateComponent(Components components,String reference) {
            return components.getHeaders().containsKey(reference);
        }
    },
    securitySchemes {
        @Override
        public boolean validateComponent(Components components,String reference) {
            return components.getSecuritySchemes().containsKey(reference);
        }
    },
    links {
        @Override
        public boolean validateComponent(Components components,String reference) {
            return components.getLinks().containsKey(reference);
        }
    },
    callbacks {
        @Override
        public boolean validateComponent(Components components,String reference) {
            return components.getCallbacks().containsKey(reference);
        }
    };


    public abstract boolean validateComponent(Components components,String reference);
}
