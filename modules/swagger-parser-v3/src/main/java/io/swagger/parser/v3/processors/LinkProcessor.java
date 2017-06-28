package io.swagger.parser.v3.processors;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.headers.Header;
import io.swagger.oas.models.links.Link;
import io.swagger.parser.v3.ResolverCache;
import io.swagger.parser.v3.models.RefFormat;


import java.util.Map;


import static io.swagger.parser.v3.util.RefUtils.computeRefFormat;

/**
 * Created by gracekarina on 23/06/17.
 */
public class LinkProcessor {
    private final ResolverCache cache;
    private final OpenAPI openApi;
    private final HeaderProcessor headerProcessor;


    public LinkProcessor(ResolverCache cache, OpenAPI openApi){
        this.cache = cache;
        this.openApi = openApi;
        this.headerProcessor = new HeaderProcessor(cache,openApi);
    }


    public Link processLink(Link link) {
        if(link.get$ref() != null){
            RefFormat refFormat = computeRefFormat(link.get$ref());
            String $ref = link.get$ref();
            Link resolved = cache.loadRef($ref, refFormat, Link.class);
            if (resolved != null) {
                return resolved;
            }

        }else if (link.getHeaders() != null){
            Map<String,Header> headers = link.getHeaders();
            for(String headerName : headers.keySet()){
                Header header = headers.get(headerName);
                Header resolvedHeader = headerProcessor.processHeader(header);
                headers.replace(headerName,header,resolvedHeader);
            }
            link.setHeaders(headers);
        }
        return link;
    }
}
