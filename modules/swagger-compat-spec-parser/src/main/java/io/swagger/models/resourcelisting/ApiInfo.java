package io.swagger.models.resourcelisting;

import io.swagger.models.SwaggerBaseModel;

public class ApiInfo extends SwaggerBaseModel {
    private String title = null;
    private String description = null;
    private String termsOfServiceUrl = null;
    private String contact = null;
    private String license = null;
    private String licenseUrl = null;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTermsOfServiceUrl() {
        return termsOfServiceUrl;
    }

    public void setTermsOfServiceUrl(String termsOfServiceUrl) {
        this.termsOfServiceUrl = termsOfServiceUrl;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ApiInfo {\n");
        sb.append("  title: ").append(title).append("\n");
        sb.append("  description: ").append(description).append("\n");
        sb.append("  termsOfServiceUrl: ").append(termsOfServiceUrl).append("\n");
        sb.append("  contact: ").append(contact).append("\n");
        sb.append("  license: ").append(license).append("\n");
        sb.append("  licenseUrl: ").append(licenseUrl).append("\n");
        sb.append("  extraFields: ").append(getExtraFields()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}

