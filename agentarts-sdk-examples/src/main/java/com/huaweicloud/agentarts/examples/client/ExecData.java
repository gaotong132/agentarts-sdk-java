package com.huaweicloud.agentarts.examples.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExecData {
    @JsonProperty("exit_code")
    private int exitCode;

    private String stdout;

    private String stderr;

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }
}
