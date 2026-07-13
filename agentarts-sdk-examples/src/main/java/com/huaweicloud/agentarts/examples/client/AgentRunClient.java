package com.huaweicloud.agentarts.examples.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.agentarts.sdk.core.APIException;
import com.huaweicloud.agentarts.sdk.service.runtime.RuntimeClient;
import io.agentscope.harness.agent.sandbox.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class AgentRunClient {
    private static final Logger LOG = LoggerFactory.getLogger(AgentRunClient.class);

    private final static String CODE_INTERPRETER_SANDBOX = "runtime-python-code";

    private final RuntimeClient runtimeClient = new RuntimeClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentRunClient(String baseUrl, String apiKey) {
        runtimeClient.setDataPlaneEndpoint(baseUrl);
        runtimeClient.setAuthToken(apiKey);
    }

    public String createSession() {
        Map<String, Object> result = runtimeClient.startSession(CODE_INTERPRETER_SANDBOX);
        if (result == null) {
            throw new RuntimeException("create session response invalid");
        }
        LOG.info("start session result, {}", result);
        SessionStartResponse response = objectMapper.convertValue(result, SessionStartResponse.class);
        if (response.getData() == null) {
            throw new RuntimeException("create session response invalid");
        }
        return response.getData().getSessionId();
    }

    public void stopSession(String sessionId) {
        try {
            runtimeClient.stopSession(CODE_INTERPRETER_SANDBOX, sessionId);
        } catch (APIException e) {
            if (e.getStatusCode() == 404) {
                LOG.warn("session:{} already not exist", sessionId);
                return;
            }
            throw new RuntimeException(e);
        }
    }

    public ExecResult executeCommand(String sessionId, String command) {
        Map<String, Object> result = this.runtimeClient.execCommand(CODE_INTERPRETER_SANDBOX, sessionId,
                List.of("sh", "-c", command), false, "", "", "", 900);

        if (result == null) {
            throw new RuntimeException("execute command response invalid");
        }

        ExecData response = objectMapper.convertValue(result, ExecData.class);
        if (response == null) {
            throw new RuntimeException("execute command response invalid");
        }
        LOG.info("execute command:{} success, result:{}", command, result);

        return new ExecResult(response.getExitCode(), response.getStdout(), response.getStderr(), false);
    }
}
