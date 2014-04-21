package com.wordnik.swagger.models.reader;

import com.wordnik.swagger.models.resourcelisting.*;
import com.wordnik.swagger.models.resourcelisting.Authorization;


import java.util.*;

public class ResourceListingParser extends SwaggerParser {

    public ResourceListingParser() {
        super();
    }

    public ResourceListing read(String json, MessageBuilder messages) {
//        try {
//            Map<String, Object> fields = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
//
//            ResourceListing resourceListing = new ResourceListing();
//
//            Object authorizations = fields.get("authorizations");
//            if (authorizations != null) {
//                Map<String, Authorization> o = parseAuthorizations((Map<String, Object>) authorizations, messages);
//                if (o.size() > 0) {
//                    resourceListing.setAuthorizations(o);
//                }
//            }
//
//            Object apiVersion = fields.get("apiVersion");
//            if (apiVersion != null)
//                resourceListing.setApiVersion(apiVersion.toString());
//            else
//                messages.append(new Message("ResourceListing.apiVersion", "apiVersion is missing", Severity.RECOMMENDED));
//
//            Object swaggerVersion = fields.get("swaggerVersion");
//            if (swaggerVersion != null)
//                resourceListing.setSwaggerVersion(swaggerVersion.toString());
//            else
//                messages.append(new Message("ResourceListing.swaggerVersion", "swaggerVersion is missing", Severity.ERROR));
//
//            Object apis = fields.get("apis");
//            if (apis != null) {
//                List<Object> o = (List<Object>) apis;
//                for (Object api : o) {
//                    if (api instanceof Map) {
//                        Map<String, Object> oo = (Map<String, Object>) api;
//                        ApiListingReference ref = readApiListingReference(oo, messages);
//                        if (ref != null)
//                            resourceListing.getApis().add(ref);
//                    }
//                }
//            } else {
//                messages.append(new Message("ResourceListing.apis", "messages not found", Severity.WARNING));
//            }
//
//            Object info = fields.get("info");
////            if (info != null) {
////                ApiInfo apiInfo = new ApiInfo();
////                Map<String, Object> o = (Map<String, Object>) info;
////                Object title = o.get("title");
////                if (title != null)
////                    apiInfo.setTitle(title.toString());
////                Object description = o.get("description");
////                if (description != null)
////                    apiInfo.setDescription(description.toString());
////                Object termsOfServiceUrl = o.get("termsOfServiceUrl");
////                if (termsOfServiceUrl != null)
////                    apiInfo.setTermsOfServiceUrl(termsOfServiceUrl.toString());
////                Object contact = o.get("contact");
////                if (contact != null)
////                    apiInfo.setContact(contact.toString());
////                Object license = o.get("license");
////                if (license != null)
////                    apiInfo.setLicense(license.toString());
////                Object licenseUrl = o.get("licenseUrl");
////                if (licenseUrl != null)
////                    apiInfo.setLicenseUrl(licenseUrl.toString());
////                resourceListing.setInfo(apiInfo);
////            }
//
//            return resourceListing;
//        } catch (Exception e) {
//            messages.append(new Message("ResourceListing", "invalid json", Severity.ERROR));
//            return null;
//        }
        return null;
    }

    private Map<String, Authorization> parseAuthorizations(Map<String, Object> authorizations, MessageBuilder messages) {
        Map<String, Authorization> output = new HashMap<>();

//        for (String key : authorizations.keySet()) {
//            Object value = authorizations.get(key);
//            if (value instanceof Map) {
//                com.wordnik.swagger.models.resourcelisting.Authorization authType = new Authorization();
//                Map<String, Object> authorization = (Map<String, Object>) value;
//
//                Object type = authorization.get("type");
//                if (type != null) {
//                    authType.setType(type.toString());
//                }
//
//                Object scopes = authorization.get("scopes");
//                if (scopes != null) {
//                    List<AuthorizationScope> s = readAuthorizationScopes((List<Map<String, Object>>) scopes, messages);
//                    if (s.size() > 0)
//                        authType.setScopes(s);
//                }
//                Object grants = authorization.get("grantTypes");
//                if (grants != null) {
//                    List<GrantTypes> s = readGrantTypes((Map<String, Object>) grants, messages);
//                    if (s.size() > 0)
//                        authType.setGrantTypes(s);
//                }
//                output.put(key, authType);
//            } else {
//                // TODO: Error handling
//            }
//        }
        return output;
    }

    private List<GrantTypes> readGrantTypes(Map<String, Object> map, MessageBuilder messages) {
        List<GrantTypes> output = new ArrayList<GrantTypes>();
//        for (String grantTypeName : map.keySet()) {
//            Object value = map.get(grantTypeName);
//            if (grantTypeName.equals("implicit")) {
//                ImplicitGrant grant = new ImplicitGrant();
//                Map<String, Object> grantDetails = (Map<String, Object>) value;
//                Object loginEndpoint = grantDetails.get("loginEndpoint");
//                if (loginEndpoint != null) {
//                    LoginEndpoint ep = new LoginEndpoint();
//                    Map<String, Object> le = (Map<String, Object>) loginEndpoint;
//                    Object url = le.get("url");
//                    if (url != null)
//                        ep.setUrl(url.toString());
//                    else
//                        messages.append(new Message("ResourceListing.authorizations.url", "url", Severity.ERROR));
//                    grant.setLoginEndpoint(ep);
//                }
//                Object tokenName = grantDetails.get("tokenName");
//                if (tokenName != null)
//                    grant.setTokenName(tokenName.toString());
//                output.add(grant);
//            } else if (grantTypeName.equals("authorization_code")) {
//                AuthorizationCodeGrant code = new AuthorizationCodeGrant();
//                Map<String, Object> grantDetails = (Map<String, Object>) value;
//                Object tokenRequestEndpoint = grantDetails.get("tokenRequestEndpoint");
//                if (tokenRequestEndpoint != null) {
//                    TokenRequestEndpoint ep = new TokenRequestEndpoint();
//                    Map<String, Object> o = (Map<String, Object>) tokenRequestEndpoint;
//                    Object url = o.get("url");
//                    if (url != null)
//                        ep.setUrl(url.toString());
//                    code.setTokenRequestEndpoint(ep);
//                    Object clientIdName = o.get("clientIdName");
//                    if (clientIdName != null)
//                        ep.setClientIdName(clientIdName.toString());
//                    Object clientSecretName = o.get("clientSecretName");
//                    if (clientSecretName != null)
//                        ep.setClientSecretName(clientSecretName.toString());
//                }
//                Object tokenEndpoint = grantDetails.get("tokenEndpoint");
//                if (tokenEndpoint != null) {
//                    TokenEndpoint te = new TokenEndpoint();
//                    Map<String, Object> o = (Map<String, Object>) tokenEndpoint;
//                    Object url = o.get("url");
//                    if (url != null)
//                        te.setUrl(url.toString());
//                    Object tokenName = o.get("tokenName");
//                    if (tokenName != null)
//                        te.setTokenName(tokenName.toString());
//                    code.setTokenEndpoint(te);
//                }
//                output.add(code);
//            }
//        }

        return output;
    }

    private ApiListingReference readApiListingReference(Map<String, Object> map, MessageBuilder messages) {
//        boolean isValid = true;
//        ApiListingReference ref = new ApiListingReference();
//        Object path = map.get("path");
//        if (path != null)
//            ref.setPath(path.toString());
//        else {
//            messages.append(new Message("ResourceListing.apis", "reference is missing path", Severity.ERROR));
//            isValid = false;
//        }
//
//        Object description = map.get("description");
//        if (description != null)
//            ref.setDescription(description.toString());
//        else {
//            messages.append(new Message("ResourceListing.apis", "reference is missing description", Severity.RECOMMENDED));
//            isValid = false;
//        }

        Object position = map.get("position");
//        if (position != null)
//            ref.setPosition(Integer.parseInt(position.toString()));
//        if (isValid)
//            return ref;
//        else
//            return null;

        return null;
    }
}