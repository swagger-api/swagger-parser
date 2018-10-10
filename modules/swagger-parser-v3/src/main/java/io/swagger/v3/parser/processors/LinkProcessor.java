package io.swagger.v3.parser.processors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;


import java.util.Map;


import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;
import static io.swagger.v3.parser.util.RefUtils.isAnExternalRefFormat;

/**
 * Created by gracekarina on 23/06/17.
 */
public class LinkProcessor {
    private final ResolverCache cache;
    private final OpenAPI openAPI;
    private final HeaderProcessor headerProcessor;
    private final ExternalRefProcessor externalRefProcessor;


    public LinkProcessor(ResolverCache cache, OpenAPI openAPI){
        this.cache = cache;
        this.openAPI = openAPI;
        this.headerProcessor = new HeaderProcessor(cache,openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }


    public void processLink(Link link) {
        if(link.get$ref() != null){
            RefFormat refFormat = computeRefFormat(link.get$ref());
            String $ref = link.get$ref();
            if (isAnExternalRefFormat(refFormat)){
                final String newRef = externalRefProcessor.processRefToExternalLink($ref, refFormat);

                if (newRef != null) {
                    link.set$ref(newRef);
                }
            }

        }else if (link.getHeaders() != null){
            Map<String,Header> headers = link.getHeaders();
            for(String headerName : headers.keySet()){
                Header header = headers.get(headerName);
                headerProcessor.processHeader(header);
            }
        }
    }
}
