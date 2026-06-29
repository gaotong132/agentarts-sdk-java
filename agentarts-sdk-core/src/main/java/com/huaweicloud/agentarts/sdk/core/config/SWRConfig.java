package com.huaweicloud.agentarts.sdk.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SWR (Software Repository for Container) configuration.
 *
 * <p>Mirrors Python {@code SWRConfig}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SWRConfig {

    @JsonProperty("organization")
    private String organization;

    @JsonProperty("repository")
    private String repository;

    @JsonProperty("organization_auto_create")
    private boolean organizationAutoCreate = false;

    @JsonProperty("repository_auto_create")
    private boolean repositoryAutoCreate = false;

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getRepository() { return repository; }
    public void setRepository(String repository) { this.repository = repository; }

    public boolean isOrganizationAutoCreate() { return organizationAutoCreate; }
    public void setOrganizationAutoCreate(boolean organizationAutoCreate) { this.organizationAutoCreate = organizationAutoCreate; }

    public boolean isRepositoryAutoCreate() { return repositoryAutoCreate; }
    public void setRepositoryAutoCreate(boolean repositoryAutoCreate) { this.repositoryAutoCreate = repositoryAutoCreate; }
}
