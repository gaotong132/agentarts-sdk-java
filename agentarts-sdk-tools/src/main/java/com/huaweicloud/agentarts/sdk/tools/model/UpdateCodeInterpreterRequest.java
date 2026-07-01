package com.huaweicloud.agentarts.sdk.tools.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Request body for updating a Code Interpreter.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateCodeInterpreterRequest {

    @JsonProperty("observability")
    private Map<String, Object> observability;

    @JsonProperty("tags")
    private List<Map<String, String>> tags;

    public UpdateCodeInterpreterRequest withObservability(Map<String, Object> observability) { this.observability = observability; return this; }
    public UpdateCodeInterpreterRequest withTags(List<Map<String, String>> tags) { this.tags = tags; return this; }

    public Map<String, Object> getObservability() { return observability; }
    public void setObservability(Map<String, Object> observability) { this.observability = observability; }
    public List<Map<String, String>> getTags() { return tags; }
    public void setTags(List<Map<String, String>> tags) { this.tags = tags; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateCodeInterpreterRequest that)) return false;
        return Objects.equals(observability, that.observability) && Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(observability, tags);
    }

    @Override
    public String toString() {
        return "UpdateCodeInterpreterRequest{observability=" + observability + ", tags=" + tags + "}";
    }
}
