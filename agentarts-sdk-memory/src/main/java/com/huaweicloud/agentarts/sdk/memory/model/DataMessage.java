package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Data message part used for summaries, offload indexes, and custom structured data. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataMessage {
    @JsonProperty("type") private String type = "data";
    @JsonProperty("kind") private String kind = "custom";
    @JsonProperty("covers") private List<String> covers;
    @JsonProperty("content") private Map<String, Object> content;
    @JsonProperty("meta") private Map<String, Object> meta;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public DataMessage withType(String type) { this.type = type; return this; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public DataMessage withKind(String kind) { this.kind = kind; return this; }
    public List<String> getCovers() { return covers; }
    public void setCovers(List<String> covers) { this.covers = covers; }
    public DataMessage withCovers(List<String> covers) { this.covers = covers; return this; }
    public Map<String, Object> getContent() { return content; }
    public void setContent(Map<String, Object> content) { this.content = content; }
    public DataMessage withContent(Map<String, Object> content) { this.content = content; return this; }
    public Map<String, Object> getMeta() { return meta; }
    public void setMeta(Map<String, Object> meta) { this.meta = meta; }
    public DataMessage withMeta(Map<String, Object> meta) { this.meta = meta; return this; }

    /** Convert to the wire representation used by the Memory service. */
    public Map<String, Object> toDict() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("kind", kind);
        if (covers != null && !covers.isEmpty()) result.put("covers", covers);
        if (content != null && !content.isEmpty()) result.put("content", content);
        if (meta != null && !meta.isEmpty()) result.put("meta", meta);
        return result;
    }
}
