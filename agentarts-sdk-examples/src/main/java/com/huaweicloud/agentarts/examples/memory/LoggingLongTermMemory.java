package com.huaweicloud.agentarts.examples.memory;

import io.agentscope.core.memory.LongTermMemory;
import io.agentscope.core.message.Msg;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * LongTermMemory 装饰器：精简打印每次 record/retrieve 的发生与规模（一行），
 * 不 dump 记忆内容，让真实运行时输出聚焦"问+答"，同时可见长期记忆何时被调用。
 * 仅用于 demo / 调试，不进 SDK 主线。
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
        int n = messages == null ? 0 : messages.size();
        System.out.println("  🧠 长期记忆 record → 写入 " + n + " 条消息");
        return delegate.record(messages);
    }

    @Override
    public Mono<String> retrieve(Msg query) {
        String q = query != null ? query.getTextContent() : "";
        String preview = q == null ? "" : (q.length() > 24 ? q.substring(0, 24) + "…" : q);
        System.out.println("  🧠 长期记忆 retrieve ← 召回中 (query=" + preview + ")");
        return delegate.retrieve(query)
                .doOnNext(out -> {
                    int lines = (out == null || out.isEmpty()) ? 0 : out.split("\n").length;
                    System.out.println("  🧠 长期记忆 retrieve → " + (lines == 0 ? "无相关记忆" : "召回 " + lines + " 条"));
                });
    }
}
