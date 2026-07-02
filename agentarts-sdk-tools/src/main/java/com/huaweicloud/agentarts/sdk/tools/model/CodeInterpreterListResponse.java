package com.huaweicloud.agentarts.sdk.tools.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response for listing code interpreters.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeInterpreterListResponse {
    @JsonProperty("items") private List<CodeInterpreterInfo> items;
    @JsonProperty("total_count") private int totalCount;

    public List<CodeInterpreterInfo> getItems() { return items; }
    public int getTotalCount() { return totalCount; }
}
