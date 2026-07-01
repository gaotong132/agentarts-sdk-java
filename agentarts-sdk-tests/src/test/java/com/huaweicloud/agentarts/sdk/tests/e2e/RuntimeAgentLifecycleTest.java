package com.huaweicloud.agentarts.sdk.tests.e2e;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runtime agent lifecycle tests — (6 tests: agent CRUD, skipped - backend prereq).
 * All tests are skipped: backend requires artifact_source_config + identity_configuration.
 */
@Tag("e2e")
@DisplayName("Runtime Agent Lifecycle Tests (Skipped)")
class RuntimeAgentLifecycleTest {

    private static final String SKIP_REASON =
            "Backend rejects create_agent without artifact_source_config (a built image) " +
            "and identity_configuration. Supply a deployable artifact to exercise this CRUD path.";

    // 1. test_find_agent_by_name
    @Test
    @DisplayName("find_agent_by_name (skipped: backend prereq)")
    void testFindAgentByName() {
        assumeTrue(false, SKIP_REASON);
    }

    // 2. test_find_agent_by_id
    @Test
    @DisplayName("find_agent_by_id (skipped: backend prereq)")
    void testFindAgentById() {
        assumeTrue(false, SKIP_REASON);
    }

    // 3. test_get_agents
    @Test
    @DisplayName("get_agents (skipped: backend prereq)")
    void testGetAgents() {
        assumeTrue(false, SKIP_REASON);
    }

    // 4. test_update_agent
    @Test
    @DisplayName("update_agent (skipped: backend prereq)")
    void testUpdateAgent() {
        assumeTrue(false, SKIP_REASON);
    }

    // 5. test_find_agent_endpoint
    @Test
    @DisplayName("find_agent_endpoint (skipped: backend prereq)")
    void testFindAgentEndpoint() {
        assumeTrue(false, SKIP_REASON);
    }

    // 6. test_update_agent_endpoint
    @Test
    @DisplayName("update_agent_endpoint (skipped: backend prereq)")
    void testUpdateAgentEndpoint() {
        assumeTrue(false, SKIP_REASON);
    }
}
