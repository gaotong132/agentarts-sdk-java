package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.tools.CodeInterpreterClient;
import com.huaweicloud.agentarts.sdk.tools.CodeSession;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Code Interpreter session tests — (1 test: full workflow - execute, upload, download).
 * Requires AGENTARTS_TEST_RUN_BILLABLE=1.
 */
@Tag("e2e")
@DisplayName("Code Interpreter Session Tests (Billable)")
class CodeInterpreterSessionTest {

    // 1. test_code_session_full_workflow
    @Test
    @DisplayName("code_session full workflow: execute, upload, download, clear")
    void testCodeSessionFullWorkflow() {
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK to run cloud tests");
        assumeTrue(E2EConfig.allowBillable(),
                "Set AGENTARTS_TEST_RUN_BILLABLE=1 to run billable tests (code-interpreter sandbox costs real money)");

        String ciName = E2EConfig.getCodeInterpreterName();
        assumeTrue(ciName != null && !ciName.isEmpty(),
                "Set AGENTARTS_TEST_CODE_INTERPRETER_NAME to the pre-provisioned code interpreter name");
        String ciApiKey = E2EConfig.getCodeInterpreterApiKey();
        assumeTrue(ciApiKey != null && !ciApiKey.isEmpty(),
                "Set HUAWEICLOUD_SDK_CODE_INTERPRETER_API_KEY");

        try (CodeSession session = CodeSession.start(
                E2EConfig.getRegion(), ciName, "aa-it-session")) {

            CodeInterpreterClient client = session.getClient();

            // execute_code
            Map<String, Object> codeResult = client.executeCode("print(1+1)");
            assertNotNull(codeResult);
            // Dig into the real output field — the MCP result envelope is
            // result.result.content[0].text (see downloadFile parsing in
            // CodeInterpreterClient). The previous toString().contains("2") was
            // vacuous: Map.toString() always contains digits from status/timestamps.
            String codeOutput = extractExecuteOutput(codeResult);
            assertTrue(codeOutput.contains("2"),
                    "execute_code result.content[0].text should contain '2' (print(1+1)); "
                            + "codeResult=" + codeResult);

            // execute_command — echo the same string we will upload, so the shell
            // output and the file content are tied by the same sentinel.
            Map<String, Object> cmdResult = client.executeCommand("echo hello-aa-it");
            assertNotNull(cmdResult);
            assertTrue(cmdResult.toString().contains("hello-aa-it"),
                    "execute_command result should contain the echoed stdout, got: " + cmdResult);

            // upload_file + download_file round-trip on the same sentinel content
            Map<String, Object> up = client.uploadFile("/home/user/aa-it-test.txt", "hello-aa-it", "test");
            assertNotNull(up);
            Object downloaded = client.downloadFile("/home/user/aa-it-test.txt");
            assertNotNull(downloaded);
            assertTrue(downloaded.toString().contains("hello-aa-it"),
                    "downloaded content should contain the uploaded sentinel bytes, got: " + downloaded);

            // get_session
            var sess = client.getSession(ciName, session.getSessionId());
            assertNotNull(sess);
            assertEquals(session.getSessionId(), sess.getSessionId(),
                    "getSession should return the same session id; got: " + sess);

            // clear_context
            Map<String, Object> cleared = client.clearContext();
            assertNotNull(cleared);
            // clearContext returns the execute_code response envelope; assert it
            // carries a real backend result (not an empty/error map).
            // CodeInterpreterClient exposes no list-context API to verify the
            // context is actually empty, so we assert the clear operation was
            // acknowledged by the backend rather than just exit-0/null.
            assertTrue(cleared.containsKey("result"),
                    "clearContext should return a response with a result field; got: " + cleared);
        }
    }

    /**
     * Extract the textual output of an {@code execute_code} invoke response.
     *
     * <p>The MCP result envelope is {@code {result: {content: [{type: "text", text: "..."}]}}}
     * (the same path {@link CodeInterpreterClient#downloadFile} navigates). This
     * returns the first content item's {@code text} field, or the empty string if
     * the envelope is absent — the caller asserts the content is non-vacuous.</p>
     */
    @SuppressWarnings("unchecked")
    private static String extractExecuteOutput(Map<String, Object> codeResult) {
        if (codeResult == null) return "";
        Object resultObj = codeResult.get("result");
        if (!(resultObj instanceof Map)) return "";
        Object contentList = ((Map<String, Object>) resultObj).get("content");
        if (!(contentList instanceof List)) return "";
        List<?> contents = (List<?>) contentList;
        if (contents.isEmpty()) return "";
        Object first = contents.get(0);
        if (!(first instanceof Map)) return "";
        Object text = ((Map<?, ?>) first).get("text");
        return text == null ? "" : text.toString();
    }
}
