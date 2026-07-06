package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.service.runtime.RuntimeClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runtime session lifecycle tests (1 test).
 *
 * <p>Tests data-plane session operations: start, exec, upload, download, stop.
 * Requires AGENTARTS_TEST_RUN_BILLABLE=1 and a pre-deployed agent.</p>
 */
@Tag("e2e")
@DisplayName("Runtime Session Lifecycle Tests (Billable)")
class RuntimeSessionLifecycleTest {

    // 1. test_runtime_session_upload_download
    @Test
    @DisplayName("runtime session: start, exec, upload, download, stop")
    void testRuntimeSessionUploadDownload() {
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK to run cloud tests");
        assumeTrue(E2EConfig.allowBillable(),
                "Set AGENTARTS_TEST_RUN_BILLABLE=1 to run billable tests (runtime invoke costs real money)");

        String agentName = E2EConfig.getRuntimeAgentName();
        assumeTrue(agentName != null && !agentName.isEmpty(),
                "Set AGENTARTS_TEST_RUNTIME_AGENT_NAME to the pre-provisioned runtime agent name");

        E2EResourceRegistry registry = new E2EResourceRegistry();

        try (RuntimeClient client = new RuntimeClient(E2EConfig.getRegion())) {
            String sessionId = "aa-it-" + UUID.randomUUID().toString().substring(0, 8);

            // 1. Start session
            Map<String, Object> started = client.startSession(agentName);
            assertNotNull(started);
            String backendSid = started.get("session_id") != null
                    ? started.get("session_id").toString() : sessionId;

            registry.register(
                    () -> client.stopSession(agentName, backendSid),
                    "runtime_session:" + agentName + ":" + backendSid);

            // 2. Exec command — assert the echoed stdout is present in the result
            Map<String, Object> cmd = client.execCommand(agentName, backendSid, "echo hello-aa-it");
            assertNotNull(cmd);
            assertTrue(cmd.toString().contains("hello-aa-it"),
                    "exec_command result should contain the echoed stdout, got: " + cmd);

            // 3. Upload file
            Map<String, Object> up = client.uploadFiles(agentName, backendSid,
                    List.of(Map.of(
                            "path", "/home/user/aa-it-test.txt",
                            "content", "hello-aa-it",
                            "description", "e2e test file")));
            assertNotNull(up);

            // 4. Download file — verify content round-trip, not just HTTP 200
            RequestResult dl = client.downloadFiles(agentName, backendSid, "/home/user/aa-it-test.txt");
            assertNotNull(dl);
            assertTrue(dl.isSuccess());
            assertNotNull(dl.getDataAsString(), "download response body should not be null");
            assertTrue(dl.getDataAsString().contains("hello-aa-it"),
                    "downloaded content should contain the uploaded bytes, got: " + dl.getDataAsString());

            // 5. Stop session
            client.stopSession(agentName, backendSid);
        } finally {
            registry.cleanupAll();
        }
    }
}
