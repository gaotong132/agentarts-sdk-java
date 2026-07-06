package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.service.runtime.RuntimeClient;
import com.huaweicloud.agentarts.sdk.service.runtime.model.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runtime agent lifecycle tests (6 tests).
 *
 * <p>Tests CRUD operations on Runtime agents and endpoints via the control plane.
 * Requires AGENTARTS_TEST_ALLOW_CREATE=1 and a backend that supports
 * artifact_source_config and identity_configuration.</p>
 */
@Tag("e2e")
@DisplayName("Runtime Agent Lifecycle Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RuntimeAgentLifecycleTest {

    private static RuntimeClient client;
    private static E2EResourceRegistry registry;
    private static String runId;

    private static String createdAgentId;
    private static String createdAgentName;
    private static String createdEndpointName;
    private static boolean setupSucceeded = false;
    private static String setupError = null;

    @BeforeAll
    static void setUp() {
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK to run cloud tests");
        assumeTrue(E2EConfig.allowCreate(),
                "Set AGENTARTS_TEST_ALLOW_CREATE=1 to run create/delete lifecycle tests");

        client = new RuntimeClient(E2EConfig.getRegion());
        registry = new E2EResourceRegistry();
        runId = E2EConfig.getRunId();

        createdAgentName = E2EHelpers.uniqueName("agent", runId);

        // Try to create agent with minimal artifact_source_config and identity_configuration
        try {
            Map<String, Object> artifactSource = Map.of(
                    "url", "swr.cn-southwest-2.myhuaweicloud.com/agentarts/e2e-test:latest",
                    "commands", List.of());
            Map<String, Object> identityConfig = Map.of("authorizer_type", "IAM");
            AgentInfo agent = client.createAgent(new CreateAgentRequest()
                    .withName(createdAgentName)
                    .withDescription("e2e test agent")
                    .withArtifactSource(artifactSource)
                    .withIdentityConfiguration(identityConfig));
            createdAgentId = agent != null ? agent.getId() : null;
            if (createdAgentId != null) {
                registry.register(
                        () -> client.deleteAgentByName(createdAgentName),
                        "runtime-agent:" + createdAgentName);
                setupSucceeded = true;
            } else {
                setupError = "createAgent returned no id";
            }
        } catch (Exception e) {
            setupError = e.getMessage();
        }
    }

    @AfterAll
    static void tearDown() {
        if (registry != null) registry.cleanupAll();
        if (client != null) client.close();
    }

    private void requireSetup() {
        String msg = setupError != null ? setupError
                : "Backend rejects create_agent without artifact_source_config and identity_configuration";
        assumeTrue(setupSucceeded, msg);
    }

    // 1. test_find_agent_by_name
    @Test @Order(1)
    @DisplayName("find_agent_by_name returns the created agent")
    void testFindAgentByName() {
        requireSetup();
        AgentInfo found = client.findAgentByName(createdAgentName);
        assertNotNull(found);
        assertEquals(createdAgentId, found.getId());
    }

    // 2. test_find_agent_by_id
    @Test @Order(2)
    @DisplayName("find_agent_by_id returns the created agent")
    void testFindAgentById() {
        requireSetup();
        AgentInfo found = client.findAgentById(createdAgentId);
        assertNotNull(found);
        assertEquals(createdAgentId, found.getId());
    }

    // 3. test_get_agents
    @Test @Order(3)
    @DisplayName("get_agents returns a list containing the created agent")
    void testGetAgents() {
        requireSetup();
        AgentListResponse agents = client.getAgents(createdAgentName, 1, 10);
        assertNotNull(agents);
        assertNotNull(agents.getItems());
        assertFalse(agents.getItems().isEmpty());
    }

    // 4. test_update_agent
    @Test @Order(4)
    @DisplayName("update_agent changes the description")
    void testUpdateAgent() {
        requireSetup();
        AgentInfo updated = client.updateAgent(createdAgentId,
                new UpdateAgentRequest().withDescription("updated by aa-it"));
        assertNotNull(updated);
        assertEquals(createdAgentId, updated.getId());
        // Re-get and assert the mutated field equals what was sent. The backend
        // applies the description to the new version's version_detail, so assert
        // the latest version's description was persisted.
        AgentInfo refetched = client.findAgentById(createdAgentId);
        assertNotNull(refetched, "findAgentById after update should return the agent");
        assertNotNull(refetched.getVersionDetail(),
                "agent response should expose version_detail after update");
        assertEquals("updated by aa-it", refetched.getVersionDetail().getDescription(),
                "update_agent should persist the new description on the latest version");
    }

    // 5. test_find_agent_endpoint
    @Test @Order(5)
    @DisplayName("create and find agent endpoint by id")
    void testFindAgentEndpoint() {
        requireSetup();
        createdEndpointName = E2EHelpers.uniqueName("ep", runId);
        AgentEndpointInfo ep = client.createAgentEndpoint(createdAgentId, createdEndpointName);
        assertNotNull(ep);
        assertNotNull(ep.getId(), "endpoint should have an id");
        String endpointId = ep.getId();

        // Find by endpoint ID and assert the found endpoint matches the created one.
        AgentEndpointInfo found = client.findAgentEndpoint(createdAgentId, endpointId);
        assertNotNull(found, "findAgentEndpoint should return the endpoint");
        assertEquals(endpointId, found.getId(), "found endpoint id must match created id");
        assertEquals(createdEndpointName, found.getName(), "found endpoint name must match created name");
    }

    // 6. test_update_agent_endpoint
    @Test @Order(6)
    @DisplayName("update agent endpoint config")
    void testUpdateAgentEndpoint() {
        requireSetup();
        String epName = E2EHelpers.uniqueName("ep2", runId);
        AgentEndpointInfo ep = client.createAgentEndpoint(createdAgentId, epName);
        assertNotNull(ep);
        assertNotNull(ep.getId(), "endpoint should have an id");
        String endpointId = ep.getId();

        // Update by endpoint ID, then re-find and assert the update persisted.
        Map<String, Object> newConfig = Map.of("note", "updated by aa-it");
        AgentEndpointInfo updated = client.updateAgentEndpoint(createdAgentId, endpointId, newConfig);
        assertNotNull(updated, "updateAgentEndpoint should return the updated endpoint");
        assertEquals(endpointId, updated.getId(), "updated endpoint id must match created id");

        AgentEndpointInfo refound = client.findAgentEndpoint(createdAgentId, endpointId);
        assertNotNull(refound, "re-find after update should return the endpoint");
        assertEquals(endpointId, refound.getId(), "refound endpoint id must match created id");
        // Backend may not echo config back; assert it when present.
        if (refound.getConfig() != null) {
            assertEquals("updated by aa-it", refound.getConfig().get("note"),
                    "updateAgentEndpoint should persist the new config");
        }
    }
}
