package com.huaweicloud.agentarts.sdk.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context manager for Code Interpreter sessions.
 *
 * <p>AutoCloseable session context that manages Code Interpreter lifecycle.
 * Creates a CodeInterpreterClient, starts a session, and stops it on close.</p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * try (CodeSession session = CodeSession.start("cn-southwest-2", "my-interpreter", "my-session")) {
 *     session.getClient().executeCode("print('hello')");
 * }
 * // Session automatically stopped on close
 * }</pre>
 */
public class CodeSession implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(CodeSession.class);

    private final CodeInterpreterClient client;
    private final boolean stopOnClose;

    private CodeSession(CodeInterpreterClient client, boolean stopOnClose) {
        this.client = client;
        this.stopOnClose = stopOnClose;
    }

    /**
     * Start a new code interpreter session.
     */
    public static CodeSession start(String region, String codeInterpreterName,
                                     String sessionName, String authType,
                                     boolean verifySsl) {
        CodeInterpreterClient client = new CodeInterpreterClient(region, null, authType, verifySsl);
        client.startSession(codeInterpreterName, sessionName);
        return new CodeSession(client, true);
    }

    public static CodeSession start(String region, String codeInterpreterName, String sessionName) {
        return start(region, codeInterpreterName, sessionName, "API_KEY", true);
    }

    public CodeInterpreterClient getClient() {
        return client;
    }

    public String getSessionId() {
        return client.getSessionId();
    }

    @Override
    public void close() {
        try {
            if (stopOnClose) {
                client.stopSession();
            }
        } catch (Exception e) {
            LOG.warn("Failed to stop code interpreter session on close: {}", e.getMessage());
        } finally {
            client.close();
        }
    }
}
