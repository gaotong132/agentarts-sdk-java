package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reference to a file, image, audio object, or another externally stored asset. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetRef {
    @JsonProperty("asset_id") private String assetId = "";
    @JsonProperty("uri") private String uri = "";
    @JsonProperty("mime") private String mime = "";
    @JsonProperty("size") private long size;
    @JsonProperty("filename") private String filename;
    @JsonProperty("meta") private Map<String, Object> meta;

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public AssetRef withAssetId(String assetId) { this.assetId = assetId; return this; }
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    public AssetRef withUri(String uri) { this.uri = uri; return this; }
    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }
    public AssetRef withMime(String mime) { this.mime = mime; return this; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public AssetRef withSize(long size) { this.size = size; return this; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public AssetRef withFilename(String filename) { this.filename = filename; return this; }
    public Map<String, Object> getMeta() { return meta; }
    public void setMeta(Map<String, Object> meta) { this.meta = meta; }
    public AssetRef withMeta(Map<String, Object> meta) { this.meta = meta; return this; }

    /** Convert to the wire representation used by the Memory service. */
    public Map<String, Object> toDict() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("asset_id", assetId);
        result.put("uri", uri);
        result.put("mime", mime);
        result.put("size", size);
        if (filename != null && !filename.isEmpty()) result.put("filename", filename);
        if (meta != null && !meta.isEmpty()) result.put("meta", meta);
        return result;
    }
}
