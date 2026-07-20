package com.huaweicloud.agentarts.examples.memory;

import io.agentscope.core.message.Msg;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * AgentStateStore 装饰器：精简打印 get/save（一行），让"短期记忆"的读取/写入在真实运行时可见。
 * 打印恢复/落盘的消息条数（State 为 AgentState 时取 context.size()）。仅用于 demo / 调试，不进 SDK。
 */
public class LoggingAgentStateStore implements AgentStateStore {

    private final AgentStateStore delegate;

    public LoggingAgentStateStore(AgentStateStore delegate) {
        this.delegate = delegate;
    }

    @Override
    public void save(String userId, String sessionId, String key, State value) {
        int n = contextSize(value);
        delegate.save(userId, sessionId, key, value);
        System.out.println("  🧠 短期记忆 save → 落盘 " + n + " 条对话");
    }

    @Override
    public void save(String userId, String sessionId, String key, List<? extends State> values) {
        delegate.save(userId, sessionId, key, values);
        System.out.println("  🧠 短期记忆 save → 落盘列表 " + values.size() + " 项");
    }

    @Override
    public <T extends State> Optional<T> get(String userId, String sessionId, String key, Class<T> type) {
        Optional<T> opt = delegate.get(userId, sessionId, key, type);
        int n = opt.map(this::contextSize).orElse(0);
        System.out.println("  🧠 短期记忆 get → 恢复 " + (n == 0 ? "空(新会话)" : n + " 条历史对话"));
        return opt;
    }

    @Override
    public <T extends State> List<T> getList(String userId, String sessionId, String key, Class<T> itemType) {
        List<T> list = delegate.getList(userId, sessionId, key, itemType);
        System.out.println("  🧠 短期记忆 getList → " + list.size() + " 项");
        return list;
    }

    @Override
    public boolean exists(String userId, String sessionId) {
        return delegate.exists(userId, sessionId);
    }

    @Override
    public void delete(String userId, String sessionId) {
        delegate.delete(userId, sessionId);
    }

    @Override
    public void delete(String userId, String sessionId, String key) {
        delegate.delete(userId, sessionId, key);
    }

    @Override
    public Set<String> listSessionIds(String userId) {
        return delegate.listSessionIds(userId);
    }

    @Override
    public void close() {
        delegate.close();
    }

    private int contextSize(State s) {
        if (s instanceof AgentState a && a.getContext() != null) {
            return a.getContext().size();
        }
        return 0;
    }
}
