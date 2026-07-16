package com.huaweicloud.agentarts.sdk.service.runtime.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Request body for executing a command in a runtime session.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecCommandRequest {

    @JsonProperty("command")
    private List<String> command;

    /**
     * Compatibility property retained for callers of older prerelease builds.
     * Chunked mode is a wire-protocol header and must not be serialized in the request body.
     */
    @Deprecated
    @JsonIgnore
    private boolean chunked;

    public ExecCommandRequest withCommand(List<String> command) {
        this.command = command;
        return this;
    }

    @Deprecated
    public ExecCommandRequest withChunked(boolean chunked) {
        this.chunked = chunked;
        return this;
    }

    public List<String> getCommand() {
        return command;
    }

    public void setCommand(List<String> command) {
        this.command = command;
    }

    @Deprecated
    public boolean isChunked() {
        return chunked;
    }

    @Deprecated
    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecCommandRequest that = (ExecCommandRequest) o;
        return chunked == that.chunked && Objects.equals(command, that.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, chunked);
    }

    @Override
    public String toString() {
        return "ExecCommandRequest{command="
                + (command == null ? "null" : "[REDACTED, arguments=" + command.size() + "]")
                + ", chunked=" + chunked + "}";
    }
}
