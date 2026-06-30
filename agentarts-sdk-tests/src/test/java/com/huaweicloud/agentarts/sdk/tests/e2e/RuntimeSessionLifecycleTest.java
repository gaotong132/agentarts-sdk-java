package com.huaweicloud.agentarts.sdk.tests.e2e;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runtime session lifecycle tests — mirrors Python test_runtime_session_lifecycle.py (1 test).
 * Requires AGENTARTS_TEST_RUN_BILLABLE=1.
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

        // Java SDK doesn't have RuntimeClient data-plane yet
        assumeTrue(false, "RuntimeClient data-plane not yet implemented in Java SDK");
    }
}
