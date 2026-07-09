package com.huaweicloud.agentarts.examples.memory;

import io.agentscope.core.memory.LongTermMemory;
import io.agentscope.core.message.Msg;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * LongTermMemory 装饰器：把每次 record/retrieve 的入参出参打印到 stdout，
 * 让"agentscope Hook 在何时调了记忆、记了什么、召回了什么"在真实运行时可见。
 *
 * <p>仅用于 demo / 调试，不进 SDK 主线。套在真实 LTM 外即可：</p>
 * <pre>{@code
 * LongTermMemory ltm = new LoggingLongTermMemory(new AgentArtsLongTermMemory(client, spaceId, actor));
 * }</pre>
 */
public class LoggingLongTermMemory implements LongTermMemory {

    private final LongTermMemory delegate;
    private final String tag;

    public LoggingLongTermMemory(LongTermMemory delegate) {
        this(delegate, "LTM");
    }

    public LoggingLongTermMemory(LongTermMemory delegate, String tag) {
        this.delegate = delegate;
        this.tag = tag;
    }

    @Override
    public Mono<Void> record(List<Msg> messages) {
        return Mono.defer(() -> {
            System.out.println("  ┌─ [" + tag + "] record() 被调用，消息数=" + (messages == null ? 0 : messages.size()));
            if (messages != null) {
                for (Msg m : messages) {
                    String role = m.getRole() != null ? m.getRole().name() : "?";
                    String text = m.getTextContent();
                    String preview = text == null ? "" : (text.length() > 60 ? text.substring(0, 60) + "…" : text);
                    System.out.println("  │   " + role + ": " + preview);
                }
            }
            return delegate.record(messages)
                    .doOnSuccess(v -> System.out.println("  └─ [" + tag + "] record() 完成 ✓"))
                    .doOnError(e -> System.out.println("  └─ [" + tag + "] record() 失败: " + e.getMessage()));
        });
    }

    @Override
    public Mono<String> retrieve(Msg query) {
        String q = query != null ? query.getTextContent() : "";
        String preview = q == null ? "" : (q.length() > 60 ? q.substring(0, 60) + "…" : q);
        System.out.println("  ┌─ [" + tag + "] retrieve() 被调用，query=" + preview);
        return delegate.retrieve(query)
                .doOnNext(out -> {
                    if (out == null || out.isEmpty()) {
                        System.out.println("  └─ [" + tag + "] retrieve() 返回空（无相关记忆）");
                    } else {
                        System.out.println("  └─ [" + tag + "] retrieve() 召回:\n" + indent(out));
                    }
                })
                .doOnError(e -> System.out.println("  └─ [" + tag + "] retrieve() 失败: " + e.getMessage()));
    }

    private static String indent(String s) {
        StringBuilder sb = new StringBuilder();
        for (String line : s.split("\n", -1)) {
            sb.append("      ").append(line).append('\n');
        }
        return sb.toString().stripTrailing();
    }
}
