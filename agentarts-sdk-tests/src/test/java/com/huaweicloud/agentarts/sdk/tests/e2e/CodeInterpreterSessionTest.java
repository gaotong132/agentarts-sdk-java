package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.tools.CodeInterpreterClient;
import com.huaweicloud.agentarts.sdk.tools.CodeSession;
import org.junit.jupiter.api.*;

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
            assertTrue(codeResult.toString().contains("2"));

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

            // clear_context
            Map<String, Object> cleared = client.clearContext();
            assertNotNull(cleared);
        }
    }
}
