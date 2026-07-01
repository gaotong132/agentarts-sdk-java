package com.huaweicloud.agentarts.sdk.service.runtime.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Request body for uploading files to a runtime session.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadFilesRequest {

    @JsonProperty("files")
    private List<Map<String, Object>> files;

    @JsonProperty("path")
    private String path;

    @JsonProperty("file_user_id")
    private Integer fileUserId;

    @JsonProperty("file_group_id")
    private Integer fileGroupId;

    @JsonProperty("file_mode")
    private String fileMode;

    public UploadFilesRequest withFiles(List<Map<String, Object>> files) {
        this.files = files;
        return this;
    }

    public UploadFilesRequest withPath(String path) {
        this.path = path;
        return this;
    }

    public UploadFilesRequest withFileUserId(Integer fileUserId) {
        this.fileUserId = fileUserId;
        return this;
    }

    public UploadFilesRequest withFileGroupId(Integer fileGroupId) {
        this.fileGroupId = fileGroupId;
        return this;
    }

    public UploadFilesRequest withFileMode(String fileMode) {
        this.fileMode = fileMode;
        return this;
    }

    public List<Map<String, Object>> getFiles() {
        return files;
    }

    public void setFiles(List<Map<String, Object>> files) {
        this.files = files;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getFileUserId() {
        return fileUserId;
    }

    public void setFileUserId(Integer fileUserId) {
        this.fileUserId = fileUserId;
    }

    public Integer getFileGroupId() {
        return fileGroupId;
    }

    public void setFileGroupId(Integer fileGroupId) {
        this.fileGroupId = fileGroupId;
    }

    public String getFileMode() {
        return fileMode;
    }

    public void setFileMode(String fileMode) {
        this.fileMode = fileMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UploadFilesRequest that = (UploadFilesRequest) o;
        return Objects.equals(files, that.files)
                && Objects.equals(path, that.path)
                && Objects.equals(fileUserId, that.fileUserId)
                && Objects.equals(fileGroupId, that.fileGroupId)
                && Objects.equals(fileMode, that.fileMode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(files, path, fileUserId, fileGroupId, fileMode);
    }

    @Override
    public String toString() {
        return "UploadFilesRequest{"
                + "files=" + files
                + ", path='" + path + "'"
                + ", fileUserId=" + fileUserId
                + ", fileGroupId=" + fileGroupId
                + ", fileMode='" + fileMode + "'"
                + "}";
    }
}
