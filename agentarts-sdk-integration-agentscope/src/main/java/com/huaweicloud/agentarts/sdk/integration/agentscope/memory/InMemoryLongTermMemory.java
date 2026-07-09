package com.huaweicloud.agentarts.sdk.integration.agentscope.memory;

import io.agentscope.core.memory.LongTermMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link LongTermMemory} 的离线/演示实现 —— 进程内存储 + 简易记忆抽取。
 *
 * <p>用途：在<b>没有云上凭据 / 没有 LLM</b> 时，让导航 demo 仍能端到端跑通
 * agentscope 长期记忆的 {@code record}/{@code retrieve} 链路。生产环境请用
 * {@link AgentArtsLongTermMemory}。</p>
 *
 * <p>如实模拟云上语义：抽取出的长期记忆按 <b>actor</b> 共享（跨实例/跨会话可召回），
 * 与 AgentArts {@code searchMemories} 按 {@code actor_id} 检索一致。</p>
 *
 * <p>抽取启发式（云上由策略引擎自动完成，此处仅做最小可演示近似）：</p>
 * <ul>
 *   <li>"我家在X / 公司在X" → semantic</li>
 *   <li>"偏好/避开/喜欢/不喜欢" → user_preference</li>
 *   <li>"昨天/上周/去了/路过" → episodic</li>
 * </ul>
 */
public class InMemoryLongTermMemory implements LongTermMemory {

    /** 按 actor 共享的长期记忆（模拟云上 actor 维度存储）。 */
    private static final Map<String, List<String[]>> ACTOR_MEMORIES = new ConcurrentHashMap<>();

    private final String actorId;

    public InMemoryLongTermMemory(String actorId) {
        this.actorId = actorId;
        ACTOR_MEMORIES.computeIfAbsent(actorId, k -> new ArrayList<>());
    }

    @Override
    public Mono<Void> record(List<Msg> messages) {
        return Mono.defer(() -> {
            doRecord(messages);
            return Mono.<Void>empty();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<String> retrieve(Msg query) {
        return Mono.fromCallable(() -> doRetrieve(query))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void doRecord(List<Msg> messages) {
        if (messages == null) {
            return;
        }
        List<String[]> store = ACTOR_MEMORIES.get(actorId);
        for (Msg m : messages) {
            String text = m.getTextContent();
            if (text == null || text.isEmpty()) {
                continue;
            }
            if (m.getRole() == MsgRole.USER) {
                extract(store, text);
            }
        }
    }

    private String doRetrieve(Msg query) {
        String q = query != null ? query.getTextContent() : "";
        if (q == null || q.isEmpty()) {
            return "";
        }
        List<String[]> store = ACTOR_MEMORIES.get(actorId);
        if (store == null || store.isEmpty()) {
            return "";
        }
        List<String[]> scored = new ArrayList<>();
        for (String[] mem : store) { // mem = [type, content]
            int overlap = overlap(q, mem[1]);
            if (overlap > 0) {
                scored.add(new String[]{mem[0], mem[1], String.format("%.2f",
                        overlap / (double) Math.max(1, q.length()))});
            }
        }
        scored.sort((a, b) -> Double.compare(
                Double.parseDouble(b[2]), Double.parseDouble(a[2])));
        if (scored.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[已召回的长期记忆 · actor=").append(actorId).append("]\n");
        int i = 1;
        for (String[] m : scored) {
            sb.append(i++).append(". (").append(m[0]).append(") ")
                    .append(m[1]).append("  [score=").append(m[2]).append("]\n");
        }
        return sb.toString();
    }

    // ======================== 简易抽取 ========================

    private void extract(List<String[]> store, String content) {
        grabLocation(store, content, "我家住在", "用户家在");
        grabLocation(store, content, "我家在", "用户家在");
        grabLocation(store, content, "家住", "用户家在");
        grabLocation(store, content, "公司在", "用户公司在");
        if (containsAny(content, "偏好", "喜欢", "不喜欢", "避开", "讨厌", "尽量不")) {
            store.add(new String[]{"user_preference", "用户偏好: " + content});
        }
        if (containsAny(content, "昨天", "上周", "上个月", "去了", "去过", "路过")) {
            store.add(new String[]{"episodic", "历史行程: " + content});
        }
    }

    private void grabLocation(List<String[]> store, String content, String trigger, String prefix) {
        int idx = content.indexOf(trigger);
        if (idx < 0) {
            return;
        }
        String rest = content.substring(idx + trigger.length()).trim();
        int end = rest.length();
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == '，' || c == ',' || c == '。' || c == '.'
                    || c == '；' || c == ';') {
                end = i;
                break;
            }
        }
        String loc = rest.substring(0, end).trim();
        if (!loc.isEmpty()) {
            store.add(new String[]{"semantic", prefix + loc});
        }
    }

    private static boolean containsAny(String s, String... keys) {
        for (String k : keys) {
            if (s.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private static int overlap(String query, String content) {
        int hit = 0;
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (Character.isWhitespace(c) || isPunct(c)) {
                continue;
            }
            if (content.indexOf(c) >= 0) {
                hit++;
            }
        }
        return hit;
    }

    private static boolean isPunct(char c) {
        return c == '，' || c == ',' || c == '。' || c == '.'
                || c == '；' || c == ';' || c == '？' || c == '?'
                || c == '！' || c == '!' || c == '、';
    }
}
