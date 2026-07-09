package com.huaweicloud.agentarts.sdk.integration.agentscope.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.model.*;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 长期记忆适配层测试：验证 {@link AgentArtsLongTermMemory} 的 record/retrieve
 * 正确路由到 AgentArts Memory 数据面（用 FakeMemoryClient 拦截，不触网），
 * 以及 {@link InMemoryLongTermMemory} 的跨实例召回。全程不依赖 LLM。
 */
class LongTermMemoryTest {

    // ========================
    // AgentArtsLongTermMemory —— 云上数据面 wiring
    // ========================

    @Nested
    @DisplayName("AgentArtsLongTermMemory: record/retrieve → AgentArts 数据面")
    class CloudTests {

        private FakeMemoryClient fake;

        @BeforeEach
        void setUp() {
            fake = new FakeMemoryClient();
        }

        @Test
        void recordConvertsMsgsToTextMessagesAndWrites() {
            AgentArtsLongTermMemory ltm = new AgentArtsLongTermMemory(fake, "space-1", "alice");
            List<Msg> msgs = List.of(
                    Msg.builder().role(MsgRole.USER).textContent("我家在朝阳公园").build(),
                    Msg.builder().role(MsgRole.ASSISTANT).textContent("好的，已记录").build());

            ltm.record(msgs).block();

            assertTrue(fake.sessionCreated, "record 应触发 createMemorySession");
            assertEquals("alice", fake.lastActorId);
            assertTrue(fake.messagesAdded, "record 应触发 addMessages");
            // 两条非空消息都被写入
            assertEquals(2, fake.storedMessages.size());
            assertEquals("user", fake.storedMessages.get(0).getRole());
            assertEquals("assistant", fake.storedMessages.get(1).getRole());
        }

        @Test
        void recordIsIdempotentOnSession() {
            AgentArtsLongTermMemory ltm = new AgentArtsLongTermMemory(fake, "space-1", "alice");
            ltm.record(List.of(Msg.builder().role(MsgRole.USER).textContent("hi").build())).block();
            int callsAfterFirst = fake.createCalls;
            ltm.record(List.of(Msg.builder().role(MsgRole.USER).textContent("again").build())).block();
            assertEquals(callsAfterFirst, fake.createCalls, "session 应只创建一次");
        }

        @Test
        void recordSkipsEmptyText() {
            AgentArtsLongTermMemory ltm = new AgentArtsLongTermMemory(fake, "space-1", "alice");
            ltm.record(List.of(
                    Msg.builder().role(MsgRole.USER).textContent("").build(),
                    Msg.builder().role(MsgRole.USER).textContent("有效内容").build())).block();
            assertEquals(1, fake.storedMessages.size());
            assertEquals("有效内容", partsText(fake.storedMessages.get(0)));
        }

        @Test
        void retrieveCallsSearchMemoriesWithActorAndTopK() {
            fake.searchResults = List.of(
                    result("用户家在国贸", "semantic"),
                    result("偏好避开高速", "user_preference"));
            AgentArtsLongTermMemory ltm = new AgentArtsLongTermMemory(fake, "space-1", "alice");

            String out = ltm.retrieve(Msg.builder().role(MsgRole.USER).textContent("回家").build()).block();

            assertEquals("回家", fake.lastSearchQuery);
            assertEquals("alice", fake.lastSearchActor);
            assertEquals(5, fake.lastSearchTopK);
            assertNotNull(out);
            assertTrue(out.contains("用户家在国贸"));
            assertTrue(out.contains("user_preference"));
        }

        @Test
        void retrieveReturnsEmptyStringWhenNoResults() {
            fake.searchResults = List.of();
            AgentArtsLongTermMemory ltm = new AgentArtsLongTermMemory(fake, "space-1", "alice");
            String out = ltm.retrieve(Msg.builder().role(MsgRole.USER).textContent("anything").build()).block();
            assertEquals("", out);
        }

        @Test
        void recordSwallowsErrorsRetrieveDegradesGracefully() {
            // 让 addMessages 抛异常，record 应 onErrorResume 吞掉，不向调用方抛
            FakeMemoryClient boom = new FakeMemoryClient() {
                @Override
                public MessageBatchResponse addMessages(String spaceId, String sessionId, List<?> messages,
                                                        Long ts, String key, boolean force) {
                    throw new RuntimeException("network down");
                }
            };
            AgentArtsLongTermMemory ltm = new AgentArtsLongTermMemory(boom, "space-1", "alice");
            assertDoesNotThrow(() -> ltm.record(
                    List.of(Msg.builder().role(MsgRole.USER).textContent("x").build())).block());
        }

        @Test
        void waitForRecallReturnsImmediatelyWhenResultsExist() {
            fake.searchResults = List.of(result("用户家在国贸", "semantic"));
            AgentArtsLongTermMemory ltm = new AgentArtsLongTermMemory(fake, "space-1", "alice");
            long t0 = System.currentTimeMillis();
            String out = ltm.waitForRecall(
                    Msg.builder().role(MsgRole.USER).textContent("回家").build(),
                    5, 5000, 100);
            assertTrue(System.currentTimeMillis() - t0 < 500, "有结果时应立即返回，不轮询");
            assertTrue(out.contains("用户家在国贸"));
        }

        @Test
        void waitForRecallReturnsEmptyOnTimeout() {
            fake.searchResults = List.of(); // 云上尚未抽取完
            AgentArtsLongTermMemory ltm = new AgentArtsLongTermMemory(fake, "space-1", "alice");
            String out = ltm.waitForRecall(
                    Msg.builder().role(MsgRole.USER).textContent("回家").build(),
                    5, 400, 150); // 短超时
            assertEquals("", out, "超时应返回空串");
        }

        @Test
        void recordUsesForceExtractByDefault() {
            AgentArtsLongTermMemory ltm = new AgentArtsLongTermMemory(fake, "space-1", "alice");
            ltm.record(List.of(Msg.builder().role(MsgRole.USER).textContent("hi").build())).block();
            assertTrue(fake.lastForceExtract, "默认应 forceExtract=true 以尽快触发云上抽取");
        }
    }

    // ========================
    // InMemoryLongTermMemory —— 跨实例召回
    // ========================

    @Nested
    @DisplayName("InMemoryLongTermMemory: 跨实例按 actor 召回")
    class InMemoryTests {

        @Test
        void crossInstanceRecallByActor() {
            String actor = "user-" + unique();
            // 实例 A：record 写入偏好与位置
            InMemoryLongTermMemory a = new InMemoryLongTermMemory(actor);
            a.record(List.of(Msg.builder().role(MsgRole.USER)
                    .textContent("我家在朝阳公园南门，公司在国贸。我偏好避开高速公路。").build())).block();

            // 全新实例 B（同 actor）：retrieve 应能召回到 A 写入的记忆
            InMemoryLongTermMemory b = new InMemoryLongTermMemory(actor);
            String out = b.retrieve(Msg.builder().role(MsgRole.USER).textContent("导航去公司").build()).block();
            assertNotNull(out);
            assertTrue(out.contains("国贸"), "应召回公司位置");
            assertTrue(out.contains("user_preference"), "应召回用户偏好");
        }

        @Test
        void actorsAreIsolated() {
            String x = "u-" + unique();
            String y = "u-" + unique();
            new InMemoryLongTermMemory(x).record(List.of(
                    Msg.builder().role(MsgRole.USER).textContent("我家在望京").build())).block();
            String out = new InMemoryLongTermMemory(y).retrieve(
                    Msg.builder().role(MsgRole.USER).textContent("家在哪").build()).block();
            assertEquals("", out, "不同 actor 记忆应隔离");
        }

        @Test
        void retrieveEmptyWhenNoMemory() {
            InMemoryLongTermMemory m = new InMemoryLongTermMemory("nobody-" + unique());
            String out = m.retrieve(Msg.builder().role(MsgRole.USER).textContent("任意").build()).block();
            assertEquals("", out);
        }
    }

    // ======================== helpers ========================

    private static int counter = 0;
    private static String unique() { return Integer.toString(++counter); }

    private static Map<String, Object> result(String content, String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("content", content);
        m.put("memory_type", type);
        m.put("score", "0.91");
        return m;
    }

    /** 拦截数据面调用的假 MemoryClient。 */
    static class FakeMemoryClient extends MemoryClient {
        boolean sessionCreated = false;
        boolean messagesAdded = false;
        boolean lastForceExtract = false;
        int createCalls = 0;
        String lastActorId;
        String lastSearchQuery;
        String lastSearchActor;
        int lastSearchTopK = -1;
        List<Map<String, Object>> searchResults = List.of();
        final List<MessageInfo> storedMessages = new ArrayList<>();
        final Map<String, List<MessageInfo>> messageStore = new ConcurrentHashMap<>();

        FakeMemoryClient() { super("cn-southwest-2", "fake-test-key"); }

        @Override
        public SessionInfo createMemorySession(String spaceId, String id, String actorId, String assistantId) {
            sessionCreated = true;
            createCalls++;
            lastActorId = actorId;
            String sid = (id != null) ? id : java.util.UUID.randomUUID().toString();
            try {
                return new ObjectMapper().readValue(
                        "{\"id\":\"" + sid + "\",\"space_id\":\"" + spaceId + "\"}", SessionInfo.class);
            } catch (Exception e) { throw new RuntimeException(e); }
        }

        @Override
        public MessageBatchResponse addMessages(String spaceId, String sessionId, List<?> messages,
                                                Long ts, String key, boolean force) {
            messagesAdded = true;
            lastForceExtract = force;
            List<MessageInfo> bucket = messageStore.computeIfAbsent(sessionId, k -> new ArrayList<>());
            int seq = bucket.size();
            for (Object msg : messages) {
                if (msg instanceof TextMessage tm) {
                    Map<String, Object> part = new HashMap<>();
                    part.put("type", "text");
                    part.put("text", tm.getContent());
                    try {
                        MessageInfo mi = new ObjectMapper().readValue(
                                "{\"id\":\"msg-" + (++seq) + "\",\"seq\":" + seq
                                        + ",\"role\":\"" + tm.getRole()
                                        + "\",\"parts\":" + new ObjectMapper().valueToTree(List.of(part)) + "}",
                                MessageInfo.class);
                        bucket.add(mi);
                        storedMessages.add(mi);
                    } catch (Exception e) { throw new RuntimeException(e); }
                }
            }
            return new MessageBatchResponse();
        }

        @Override
        public MemorySearchResponse searchMemories(String spaceId, MemorySearchFilter filters) {
            if (filters != null) {
                lastSearchQuery = filters.getQuery();
                lastSearchActor = filters.getActorId();
                lastSearchTopK = filters.getTopK() != null ? filters.getTopK() : -1;
            }
            try {
                String json = new ObjectMapper().writeValueAsString(Map.of(
                        "results", searchResults, "total", searchResults.size(), "query", "q"));
                return new ObjectMapper().readValue(json, MemorySearchResponse.class);
            } catch (Exception e) { throw new RuntimeException(e); }
        }

        @Override
        public void close() { /* no-op */ }
    }

    /** 测试辅助：从 MessageInfo 取文本。 */
    @SuppressWarnings("unchecked")
    static String partsText(MessageInfo mi) {
        if (mi == null || mi.getParts() == null) return "";
        StringBuilder sb = new StringBuilder();
        for (Object p : mi.getParts()) {
            if (p instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) p;
                if ("text".equals(m.get("type")) && m.get("text") != null) sb.append(m.get("text"));
            }
        }
        return sb.toString();
    }
}
