package com.huaweicloud.agentarts.sdk.integration.agentscope.memory;

import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.model.MemorySearchFilter;
import com.huaweicloud.agentarts.sdk.memory.model.MemorySearchResponse;
import com.huaweicloud.agentarts.sdk.memory.model.SessionInfo;
import com.huaweicloud.agentarts.sdk.memory.model.TextMessage;
import io.agentscope.core.memory.LongTermMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * agentscope {@link LongTermMemory} 的 AgentArts 云上实现。
 *
 * <p>这是接入 AgentArts 云上记忆的<b>核心适配器</b>。agentscope 的长期记忆契约只有两个方法，
 * 本实现把它们映射到 AgentArts Memory 数据面：</p>
 *
 * <table>
 *   <tr><th>agentscope LongTermMemory</th><th>AgentArts Memory 数据面</th></tr>
 *   <tr><td>{@link #record(List)}</td><td>{@code addMessages(TextMessage)} —— 写入对话消息，<b>触发云上自动抽取</b> semantic/user_preference/episodic 记忆</td></tr>
 *   <tr><td>{@link #retrieve(Msg)}</td><td>{@code searchMemories(query, actor_id, topK)} —— 按 actor <b>跨会话语义召回</b>，格式化为字符串注入 Prompt</td></tr>
 * </table>
 *
 * <h3>三种使用模式（由 ReActAgent 的 {@code longTermMemoryMode} 决定）</h3>
 * <ul>
 *   <li><b>STATIC_CONTROL</b> —— agentscope 的 {@code StaticLongTermMemoryHook} 在
 *       PRE/POST_CALL 等事件点<b>自动</b>调用 {@code record}/{@code retrieve}，
 *       并把召回结果 {@code appendSystemContent} 注入 system 消息。无需 LLM 决策。</li>
 *   <li><b>AGENT_CONTROL</b> —— {@code LongTermMemoryTools}（{@code recordToMemory}/{@code retrieveFromMemory}，@Tool 注解）
 *       被注册为工具，由 LLM 决定何时记忆/召回。</li>
 *   <li><b>BOTH</b> —— 两者兼有。</li>
 * </ul>
 *
 * <p>本类只实现 {@code record}/{@code retrieve} 两个方法，模式/hook/工具全部复用 agentscope 原生机制，
 * 通过 {@code ReActAgent.builder().longTermMemory(this).longTermMemoryMode(...)} 接入。</p>
 *
 * <h3>actor 维度</h3>
 * 记忆按 {@code actorId}（用户标识）在云上隔离与召回。一个 LTM 实例绑定一个 actor；
 * 多用户场景应为每个用户构造独立 LTM 实例（或通过 PRE_CALL 钩子把 RuntimeContext.userId
 * 透传到上下文相关实现）。
 */
public class AgentArtsLongTermMemory implements LongTermMemory {

    private static final Logger LOG = LoggerFactory.getLogger(AgentArtsLongTermMemory.class);

    private final MemoryClient client;
    private final String spaceId;
    private final String actorId;
    private final String assistantId;
    private final int retrieveTopK;
    private final boolean forceExtract;
    private volatile String sessionId;

    /**
     * @param client    已配置好 API Key 的 MemoryClient（数据面）
     * @param spaceId   Memory Space ID
     * @param actorId   用户/Actor 标识，云上按 actor 隔离与召回
     */
    public AgentArtsLongTermMemory(MemoryClient client, String spaceId, String actorId) {
        this(client, spaceId, actorId, null, 5, true);
    }

    public AgentArtsLongTermMemory(MemoryClient client, String spaceId, String actorId,
                                   String assistantId, int retrieveTopK) {
        this(client, spaceId, actorId, assistantId, retrieveTopK, true);
    }

    /**
     * @param forceExtract 写入消息时是否强制触发云上抽取（默认 true）。
     *                     云上抽取是<b>异步</b>的：record 只写消息，Memory 资源由后台策略引擎提炼，
     *                     有秒级~分钟级延迟。forceExtract=true 让写入后尽快进入抽取流程，
     *                     但<b>不保证 retrieve 时一定已抽取完</b>——见 {@link #waitForRecall}。
     */
    public AgentArtsLongTermMemory(MemoryClient client, String spaceId, String actorId,
                                   String assistantId, int retrieveTopK, boolean forceExtract) {
        this.client = client;
        this.spaceId = spaceId;
        this.actorId = actorId;
        this.assistantId = assistantId;
        this.retrieveTopK = retrieveTopK;
        this.forceExtract = forceExtract;
    }

    /**
     * 写入对话消息到云上 Memory 消息面，触发云上记忆抽取。
     * 对应 agentscope LongTermMemory.record。
     */
    @Override
    public Mono<Void> record(List<Msg> messages) {
        return Mono.defer(() -> {
            doRecord(messages);
            return Mono.<Void>empty();
        }).subscribeOn(Schedulers.boundedElastic())
          .onErrorResume(e -> {
              LOG.warn("AgentArtsLongTermMemory.record failed: {}", e.toString());
              return Mono.empty();
          });
    }

    /**
     * 按 actor 跨会话语义召回长期记忆，返回可直接注入 Prompt 的字符串。
     * 对应 agentscope LongTermMemory.retrieve。
     */
    @Override
    public Mono<String> retrieve(Msg query) {
        return Mono.fromCallable(() -> doRetrieve(query))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    LOG.warn("AgentArtsLongTermMemory.retrieve failed: {}", e.toString());
                    return Mono.just("");
                });
    }

    // ======================== 内部实现 ========================

    /**
     * 轮询召回，直到拿到非空结果或超时。用于 demo 在 record 后等云上抽取完成。
     *
     * <p>云上抽取是异步的：record 写消息后，Memory 资源由后台策略引擎提炼，有延迟。
     * 立即 retrieve 可能返回空。本方法按 intervalMs 间隔重试，直到 recall 非空或达到 timeoutMs。</p>
     *
     * @return 召回字符串；超时仍为空则返回 ""
     */
    public String waitForRecall(Msg query, int topK, long timeoutMs, long intervalMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        String out = "";
        int attempt = 0;
        while (System.currentTimeMillis() < deadline) {
            attempt++;
            out = doRetrieve(query);
            if (out != null && !out.isEmpty()) {
                LOG.info("waitForRecall: 命中（第{}次轮询）", attempt);
                return out;
            }
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        LOG.warn("waitForRecall: 超时（{}ms，共{}次）仍未召回，云上抽取可能仍在进行", timeoutMs, attempt);
        return out;
    }

    private void doRecord(List<Msg> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        ensureSession();
        List<TextMessage> batch = new ArrayList<>();
        for (Msg m : messages) {
            String text = m.getTextContent();
            if (text == null || text.isEmpty()) {
                continue;
            }
            TextMessage tm = new TextMessage(roleOf(m.getRole()), text);
            tm.setActorId(actorId);
            if (assistantId != null) {
                tm.setAssistantId(assistantId);
            }
            batch.add(tm);
        }
        if (!batch.isEmpty()) {
            // forceExtract=true：写入后强制触发云上抽取（而非等定时任务），尽量缩短 record→可 retrieve 的窗口
            client.addMessages(spaceId, sessionId, batch, null, null, forceExtract);
        }
    }

    private String doRetrieve(Msg query) {
        String q = query != null ? query.getTextContent() : "";
        if (q == null || q.isEmpty()) {
            return "";
        }
        MemorySearchFilter filter = new MemorySearchFilter();
        filter.setQuery(q);
        filter.setActorId(actorId); // 跨会话、仅召回当前 actor 的长期记忆
        filter.setTopK(retrieveTopK);

        MemorySearchResponse resp = client.searchMemories(spaceId, filter);
        return formatResults(resp);
    }

    private String formatResults(MemorySearchResponse resp) {
        if (resp == null || resp.getResults() == null || resp.getResults().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[已从云上召回的长期记忆 · actor=").append(actorId).append("]\n");
        int i = 1;
        for (Map<String, Object> r : resp.getResults()) {
            // 真实 schema: {"record": {"content":..., "memory_type":...}, "score":...}
            // 兼容 flat schema: {"content":..., "memory_type":..., "score":...}
            Object recordObj = r.get("record");
            Map<String, Object> rec = (recordObj instanceof Map)
                    ? (Map<String, Object>) recordObj : r;
            Object content = rec.get("content");
            Object type = rec.get("memory_type");
            Object score = r.get("score");
            if (content == null) {
                continue;
            }
            sb.append(i++).append(". (")
                    .append(type != null ? type : "memory")
                    .append(") ").append(content);
            if (score != null) {
                sb.append("  [score=").append(score).append("]");
            }
            sb.append("\n");
        }
        // 所有项都无 content 时，去掉空 header
        if (i == 1) {
            return "";
        }
        return sb.toString();
    }

    private void ensureSession() {
        if (sessionId != null) {
            return;
        }
        synchronized (this) {
            if (sessionId != null) {
                return;
            }
            // sessionId 由服务端生成（UUID）；按 actor 聚合，便于跨会话召回
            SessionInfo info = client.createMemorySession(spaceId, null, actorId, assistantId);
            if (info == null || info.getId() == null) {
                throw new IllegalStateException("createMemorySession returned null id");
            }
            this.sessionId = info.getId();
        }
    }

    private static String roleOf(MsgRole role) {
        if (role == null) {
            return "user";
        }
        switch (role) {
            case USER: return "user";
            case ASSISTANT: return "assistant";
            case SYSTEM: return "system";
            case TOOL: return "tool";
            default: return role.name().toLowerCase();
        }
    }

    /** 暴露给测试 / 可观测：当前绑定的 cloud sessionId（首次 record 后非空）。 */
    public String getSessionId() {
        return sessionId;
    }

    public String getActorId() {
        return actorId;
    }
}
