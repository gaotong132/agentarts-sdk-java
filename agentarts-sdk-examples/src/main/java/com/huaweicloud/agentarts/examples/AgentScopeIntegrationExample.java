package com.huaweicloud.agentarts.examples;

import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.integration.agentscope.runtime.AgentscopeRuntimeHost;
import com.huaweicloud.agentarts.sdk.integration.agentscope.state.MemoryAgentStateStore;
import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.*;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.model.OpenAIChatModel;
import reactor.core.publisher.Flux;

/**
 * AgentScope 集成完整示例 — 演示 AgentArts SDK 与 agentscope-java 的深度集成。
 *
 * <p>使用 OpenAI 兼容模型（通过 {@link OpenAIChatModel}），可对接任何 OpenAI 兼容 API，
 * 包括华为云 MaaS、OpenAI、DeepSeek 等。</p>
 *
 * <p>本示例涵盖以下核心能力：</p>
 * <ol>
 *   <li><b>ReActAgent 构建</b> — 使用 OpenAI 兼容模型、自定义工具、状态存储</li>
 *   <li><b>工具注册</b> — 通过 @Tool 注解声明工具，自动注册到 Toolkit</li>
 *   <li><b>流式事件</b> — 通过 streamEvents() 获取实时推理过程</li>
 *   <li><b>会话上下文</b> — 通过 RuntimeContext 传递用户/会话信息</li>
 *   <li><b>状态持久化</b> — 可选使用 AgentArts Memory 作为后端存储</li>
 *   <li><b>Runtime 服务</b> — 可选通过 AgentscopeRuntimeHost 暴露 HTTP 服务</li>
 * </ol>
 *
 * <h2>环境变量：</h2>
 * <ul>
 *   <li>{@code OPENAI_API_KEY} — <b>必需</b>，模型 API Key</li>
 *   <li>{@code OPENAI_BASE_URL} — 可选，自定义 API 端点（对接华为云 MaaS 等 OpenAI 兼容服务）</li>
 *   <li>{@code OPENAI_MODEL_NAME} — 可选，模型名称（默认: gpt-4o）</li>
 *   <li>{@code AGENTARTS_MEMORY_API_KEY} — 可选，AgentArts Memory 数据面 API Key</li>
 *   <li>{@code AGENTARTS_MEMORY_SPACE_ID} — 可选，AgentArts Memory 空间 ID</li>
 *   <li>{@code RUNTIME_MODE} — 可选，设为 "server" 启用 HTTP Runtime 模式</li>
 * </ul>
 *
 * <h2>运行方式：</h2>
 * <pre>
 * # 使用 OpenAI
 * export OPENAI_API_KEY=sk-xxx
 * mvn compile exec:java -pl agentarts-sdk-examples \
 *   -Dexec.mainClass="com.huaweicloud.agentarts.examples.AgentScopeIntegrationExample"
 *
 * # 使用华为云 MaaS（OpenAI 兼容接口）
 * export OPENAI_API_KEY=your-maas-key
 * export OPENAI_BASE_URL=https://maas.cn-north-4.myhuaweicloud.com/v1
 * export OPENAI_MODEL_NAME=deepseek-v3
 * mvn compile exec:java -pl agentarts-sdk-examples \
 *   -Dexec.mainClass="com.huaweicloud.agentarts.examples.AgentScopeIntegrationExample"
 *
 * # 带 AgentArts Memory 持久化
 * export OPENAI_API_KEY=sk-xxx
 * export AGENTARTS_MEMORY_API_KEY=your-memory-api-key
 * export AGENTARTS_MEMORY_SPACE_ID=your-space-id
 * mvn compile exec:java -pl agentarts-sdk-examples \
 *   -Dexec.mainClass="com.huaweicloud.agentarts.examples.AgentScopeIntegrationExample"
 *
 * # HTTP Runtime 服务模式
 * export OPENAI_API_KEY=sk-xxx
 * export RUNTIME_MODE=server
 * mvn compile exec:java -pl agentarts-sdk-examples \
 *   -Dexec.mainClass="com.huaweicloud.agentarts.examples.AgentScopeIntegrationExample"
 * </pre>
 */
public class AgentScopeIntegrationExample {

    // ========================================================================
    // 1. 工具定义 — 使用 @Tool 注解声明可供 Agent 调用的工具
    // ========================================================================

    /**
     * 数学计算工具集。
     *
     * <p>agentscope 通过反射扫描 @Tool 注解方法，自动生成 JSON Schema
     * 供 LLM 决策何时调用哪个工具。</p>
     */
    public static class MathTools {

        @Tool(name = "calculate", description = "执行基本数学运算。支持加减乘除和括号。")
        public String calculate(
                @ToolParam(name = "expression", description = "数学表达式，例如: 2 + 3 * 4") String expression) {
            try {
                double result = evaluateExpression(expression);
                return String.format("计算结果: %s = %s", expression, formatResult(result));
            } catch (Exception e) {
                return "计算失败: " + e.getMessage();
            }
        }

        @Tool(name = "unit_convert", description = "单位换算。支持温度、长度、重量等常见单位转换。")
        public String unitConvert(
                @ToolParam(name = "value", description = "数值") double value,
                @ToolParam(name = "from_unit", description = "源单位，如: celsius, fahrenheit, km, mile, kg, lb") String fromUnit,
                @ToolParam(name = "to_unit", description = "目标单位") String toUnit) {
            String key = fromUnit.toLowerCase() + "_to_" + toUnit.toLowerCase();
            return switch (key) {
                case "celsius_to_fahrenheit" ->
                        String.format("%.2f°C = %.2f°F", value, value * 9 / 5 + 32);
                case "fahrenheit_to_celsius" ->
                        String.format("%.2f°F = %.2f°C", value, (value - 32) * 5 / 9);
                case "km_to_mile" ->
                        String.format("%.2f km = %.2f mile", value, value * 0.621371);
                case "mile_to_km" ->
                        String.format("%.2f mile = %.2f km", value, value * 1.60934);
                case "kg_to_lb" ->
                        String.format("%.2f kg = %.2f lb", value, value * 2.20462);
                case "lb_to_kg" ->
                        String.format("%.2f lb = %.2f kg", value, value * 0.453592);
                default -> "不支持的转换: " + fromUnit + " → " + toUnit;
            };
        }

        private double evaluateExpression(String expr) {
            expr = expr.replaceAll("\\s+", "");
            return parseExpression(expr, new int[]{0});
        }

        private double parseExpression(String s, int[] pos) {
            double result = parseTerm(s, pos);
            while (pos[0] < s.length()) {
                char op = s.charAt(pos[0]);
                if (op == '+' || op == '-') {
                    pos[0]++;
                    double term = parseTerm(s, pos);
                    result = (op == '+') ? result + term : result - term;
                } else {
                    break;
                }
            }
            return result;
        }

        private double parseTerm(String s, int[] pos) {
            double result = parseFactor(s, pos);
            while (pos[0] < s.length()) {
                char op = s.charAt(pos[0]);
                if (op == '*' || op == '/') {
                    pos[0]++;
                    double factor = parseFactor(s, pos);
                    result = (op == '*') ? result * factor : result / factor;
                } else {
                    break;
                }
            }
            return result;
        }

        private double parseFactor(String s, int[] pos) {
            if (pos[0] < s.length() && s.charAt(pos[0]) == '(') {
                pos[0]++;
                double result = parseExpression(s, pos);
                if (pos[0] < s.length() && s.charAt(pos[0]) == ')') pos[0]++;
                return result;
            }
            boolean negative = pos[0] < s.length() && s.charAt(pos[0]) == '-';
            if (negative) pos[0]++;
            int start = pos[0];
            while (pos[0] < s.length() && (Character.isDigit(s.charAt(pos[0])) || s.charAt(pos[0]) == '.')) {
                pos[0]++;
            }
            double val = Double.parseDouble(s.substring(start, pos[0]));
            return negative ? -val : val;
        }

        private String formatResult(double value) {
            if (value == Math.floor(value) && !Double.isInfinite(value)) {
                return String.valueOf((long) value);
            }
            return String.valueOf(value);
        }
    }

    /**
     * 信息查询工具集。
     */
    public static class InfoTools {

        @Tool(name = "get_current_time", description = "获取当前日期和时间。")
        public String getCurrentTime() {
            return "当前时间: " + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        @Tool(name = "search_knowledge", description = "搜索知识库获取指定主题的信息。")
        public String searchKnowledge(
                @ToolParam(name = "query", description = "搜索查询关键词") String query) {
            // 模拟知识库搜索 — 实际项目中可对接 ES、向量数据库等
            return String.format("关于「%s」的搜索结果: AgentArts 是华为云的智能体开发平台，"
                    + "提供 Memory、Code Interpreter、MCP Gateway 等核心能力，"
                    + "支持通过 Java/Python SDK 快速构建 AI Agent 应用。", query);
        }
    }

    // ========================================================================
    // 2. 主程序入口
    // ========================================================================

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("错误: 请设置环境变量 OPENAI_API_KEY");
            System.err.println("  export OPENAI_API_KEY=sk-xxx");
            System.err.println();
            System.err.println("可选: 设置 OPENAI_BASE_URL 对接华为云 MaaS 等 OpenAI 兼容服务");
            System.err.println("  export OPENAI_BASE_URL=https://maas.cn-north-4.myhuaweicloud.com/v1");
            System.exit(1);
        }

        String mode = System.getenv().getOrDefault("RUNTIME_MODE", "interactive");
        String baseUrl = System.getenv().getOrDefault("OPENAI_BASE_URL", "(默认 OpenAI)");
        String modelName = System.getenv().getOrDefault("OPENAI_MODEL_NAME", "gpt-4o");

        System.out.println("=".repeat(60));
        System.out.println("AgentArts × AgentScope 集成示例");
        System.out.println("=".repeat(60));
        System.out.println("运行模式: " + mode);
        System.out.println("模型 API: " + baseUrl);
        System.out.println("模型名称: " + modelName);
        System.out.println("=".repeat(60));

        // 构建 Agent
        ReActAgent agent = buildAgent(apiKey, modelName);

        try {
            if ("server".equalsIgnoreCase(mode)) {
                runServerMode(agent);
            } else {
                runInteractiveMode(agent);
            }
        } finally {
            agent.close();
        }
    }

    // ========================================================================
    // 3. Agent 构建 — 组装模型、工具、状态存储
    // ========================================================================

    /**
     * 构建 ReActAgent 实例。
     *
     * <p>关键步骤：</p>
     * <ol>
     *   <li>创建 {@link OpenAIChatModel}，支持自定义 baseUrl 对接 OpenAI 兼容 API</li>
     *   <li>创建 {@link Toolkit} 并注册 @Tool 注解的工具</li>
     *   <li>选择状态存储（AgentArts Memory 持久化 或 内存存储）</li>
     *   <li>构建 {@link ReActAgent}（推理-行动循环）</li>
     * </ol>
     */
    static ReActAgent buildAgent(String apiKey, String modelName) {
        // --- 模型配置 ---
        // OpenAIChatModel 支持自定义 baseUrl，可对接任何 OpenAI 兼容 API：
        //   - OpenAI:         不设置 baseUrl（使用默认 https://api.openai.com/v1）
        //   - 华为云 MaaS:    OPENAI_BASE_URL=https://maas.cn-north-4.myhuaweicloud.com/v1
        //   - DeepSeek:       OPENAI_BASE_URL=https://api.deepseek.com/v1
        //   - 本地 Ollama:    OPENAI_BASE_URL=http://localhost:11434/v1
        OpenAIChatModel.Builder modelBuilder = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .stream(true);

        String baseUrl = System.getenv("OPENAI_BASE_URL");
        if (baseUrl != null && !baseUrl.isEmpty()) {
            modelBuilder.baseUrl(baseUrl);
        }

        OpenAIChatModel model = modelBuilder.build();
        System.out.println("[模型] " + modelName + (baseUrl != null ? " @ " + baseUrl : ""));

        // --- 工具注册 ---
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new MathTools());
        toolkit.registerTool(new InfoTools());
        System.out.println("[工具] 已注册: calculate, unit_convert, get_current_time, search_knowledge");

        // --- 状态存储 ---
        AgentStateStore stateStore = createStateStore();

        // --- 构建 ReActAgent ---
        ReActAgent agent = ReActAgent.builder()
                .name("AgentArts-Assistant")
                .sysPrompt(
                        "你是一个智能助手，集成了 AgentArts SDK 和 AgentScope 框架。\n"
                                + "你可以使用以下工具帮助用户解决问题：\n"
                                + "- calculate: 数学计算\n"
                                + "- unit_convert: 单位换算\n"
                                + "- get_current_time: 查询当前时间\n"
                                + "- search_knowledge: 搜索知识库\n"
                                + "请用中文回答用户的问题，并在需要时主动使用工具获取信息。")
                .model(model)
                .toolkit(toolkit)
                .stateStore(stateStore)
                .defaultSessionId("demo-session")
                .maxIters(5)
                .build();

        System.out.println("[Agent] 构建完成: AgentArts-Assistant (maxIters=5)");
        return agent;
    }

    /**
     * 创建状态存储。
     * 优先使用 AgentArts Memory（需要配置环境变量），否则使用内存存储。
     */
    private static AgentStateStore createStateStore() {
        String memoryApiKey = System.getenv("AGENTARTS_MEMORY_API_KEY");
        String memorySpaceId = System.getenv("AGENTARTS_MEMORY_SPACE_ID");

        if (memoryApiKey != null && !memoryApiKey.isEmpty()
                && memorySpaceId != null && !memorySpaceId.isEmpty()) {
            String region = System.getenv().getOrDefault("HUAWEICLOUD_SDK_REGION", "cn-southwest-2");
            MemoryClient memoryClient = new MemoryClient(region, memoryApiKey);
            System.out.println("[存储] 使用 AgentArts Memory (spaceId=" + memorySpaceId + ")");
            return new MemoryAgentStateStore(memoryClient, memorySpaceId);
        }

        System.out.println("[存储] 使用内存存储 (设置 AGENTARTS_MEMORY_API_KEY + AGENTARTS_MEMORY_SPACE_ID 启用持久化)");
        return new InMemoryAgentStateStore();
    }

    // ========================================================================
    // 4. 交互模式 — 直接对话 + 流式事件输出
    // ========================================================================

    /**
     * 交互模式：直接调用 Agent 并展示流式事件。
     *
     * <p>演示两种调用方式：</p>
     * <ul>
     *   <li>{@code call()} — 同步调用，返回最终结果</li>
     *   <li>{@code streamEvents()} — 流式调用，实时输出推理过程</li>
     * </ul>
     */
    static void runInteractiveMode(ReActAgent agent) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("交互模式：直接对话");
        System.out.println("-".repeat(60));

        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId("demo-session")
                .userId("demo-user")
                .build();

        // --- 演示 1: 简单问答（不使用工具）---
        System.out.println("\n▶ 测试 1: 简单问答");
        System.out.println("用户: 你好，请做个自我介绍");
        Msg reply1 = agent.call("你好，请做个自我介绍", ctx).block();
        System.out.println("助手: " + (reply1 != null ? reply1.getTextContent() : "(无回复)"));

        // --- 演示 2: 工具调用（数学计算）— 使用流式事件 ---
        System.out.println("\n" + "-".repeat(40));
        System.out.println("\n▶ 测试 2: 工具调用 — 数学计算（流式）");
        demoStreamingCall(agent, "帮我算一下 (125 + 375) * 2 / 10 等于多少", ctx);

        // --- 演示 3: 多工具调用（单位换算 + 时间查询）---
        System.out.println("\n" + "-".repeat(40));
        System.out.println("\n▶ 测试 3: 多工具调用（流式）");
        demoStreamingCall(agent, "现在几点了？另外帮我换算一下 100 公里等于多少英里", ctx);

        // --- 演示 4: 知识库搜索 ---
        System.out.println("\n" + "-".repeat(40));
        System.out.println("\n▶ 测试 4: 知识库搜索（流式）");
        demoStreamingCall(agent, "帮我查一下 AgentArts 是什么", ctx);

        // --- 演示 5: 上下文记忆（多轮对话）---
        System.out.println("\n" + "-".repeat(40));
        System.out.println("\n▶ 测试 5: 上下文记忆");
        System.out.println("用户: 我叫小明，我今年 25 岁");
        agent.call("我叫小明，我今年 25 岁", ctx).block();
        System.out.println("（已记住用户信息）");

        System.out.println("用户: 我叫什么名字？我多大了？");
        Msg reply5 = agent.call("我叫什么名字？我多大了？", ctx).block();
        System.out.println("助手: " + (reply5 != null ? reply5.getTextContent() : "(无回复)"));

        System.out.println("\n" + "=".repeat(60));
        System.out.println("交互模式演示完成");
        System.out.println("=".repeat(60));
    }

    /**
     * 流式调用 Agent 并实时展示事件。
     *
     * <p>{@link io.agentscope.core.event.AgentEvent} 事件流的生命周期：</p>
     * <pre>
     * AgentStartEvent → [ModelCallStart → TextBlockDelta* → ModelCallEnd]
     *                  → [ToolCallStart → ToolCallEnd → ToolResultStart → ToolResultEnd]
     *                  → ... (多轮推理-行动循环)
     *                  → AgentResultEvent → AgentEndEvent
     * </pre>
     */
    private static void demoStreamingCall(ReActAgent agent, String userInput, RuntimeContext ctx) {
        System.out.println("用户: " + userInput);
        System.out.println();

        Flux<AgentEvent> events = agent.streamEvents(new UserMessage(userInput), ctx);

        events.doOnNext(event -> {
            if (event instanceof AgentStartEvent e) {
                System.out.println("  [开始] 会话: " + e.getSessionId());
            } else if (event instanceof ModelCallStartEvent) {
                System.out.println("  [模型调用] 开始...");
            } else if (event instanceof TextBlockDeltaEvent e) {
                System.out.print(e.getDelta());
            } else if (event instanceof ToolCallStartEvent e) {
                System.out.println("\n  [工具调用] " + e.getToolCallName() + " (id=" + e.getToolCallId() + ")");
            } else if (event instanceof ToolCallEndEvent) {
                System.out.println("  [工具调用] 完成");
            } else if (event instanceof ToolResultStartEvent) {
                System.out.print("  [工具结果] ");
            } else if (event instanceof ToolResultTextDeltaEvent e) {
                System.out.print(e.getDelta());
            } else if (event instanceof ToolResultEndEvent) {
                System.out.println();
            } else if (event instanceof AgentResultEvent e) {
                Msg result = e.getResult();
                if (result != null && !result.getTextContent().isEmpty()) {
                    System.out.println("\n  [最终回复] " + result.getTextContent());
                }
            } else if (event instanceof AgentEndEvent) {
                System.out.println("  [结束]");
            }
        }).blockLast();

        System.out.println();
    }

    // ========================================================================
    // 5. Runtime 服务模式 — 通过 AgentArts Runtime 暴露 HTTP 接口
    // ========================================================================

    /**
     * Runtime 服务模式：将 Agent 暴露为 HTTP 服务。
     *
     * <p>通过 {@link AgentscopeRuntimeHost} 桥接 AgentArts Runtime 与 agentscope，
     * 使外部系统可以通过 HTTP POST /invocations 调用 Agent。</p>
     *
     * <p>支持的请求格式：</p>
     * <pre>
     * POST http://localhost:8080/invocations
     * Content-Type: application/json
     *
     * {"message": "帮我计算 100 + 200"}
     * </pre>
     *
     * <p>支持的 Header：</p>
     * <ul>
     *   <li>{@code x-hw-agentarts-session-id} — 会话 ID</li>
     *   <li>{@code X-HW-AgentGateway-User-Id} — 用户 ID</li>
     * </ul>
     */
    static void runServerMode(ReActAgent agent) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();

        // AgentscopeRuntimeHost 桥接 RequestContext → RuntimeContext
        new AgentscopeRuntimeHost(app, (payload, runtimeCtx) -> {
            String message = (String) payload.getOrDefault("message", "");
            if (message.isEmpty()) {
                return java.util.Map.of("error", "请提供 message 字段");
            }

            String userId = runtimeCtx.getUserId() != null ? runtimeCtx.getUserId() : "default";
            String sessionId = runtimeCtx.getSessionId() != null
                    ? runtimeCtx.getSessionId() : "default-session";

            RuntimeContext ctx = RuntimeContext.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .build();

            // 同步调用 Agent
            Msg result = agent.call(message, ctx).block();
            String reply = result != null ? result.getTextContent() : "";

            return java.util.Map.of(
                    "reply", reply,
                    "userId", userId,
                    "sessionId", sessionId
            );
        });

        // 自定义健康检查
        app.setPingHandler(() -> PingStatus.HEALTHY);

        System.out.println("\n" + "-".repeat(60));
        System.out.println("Runtime 服务模式");
        System.out.println("-".repeat(60));
        System.out.println("启动 HTTP 服务...");
        System.out.println("  健康检查:   GET  http://localhost:" + port + "/ping");
        System.out.println("  调用 Agent: POST http://localhost:" + port + "/invocations");
        System.out.println("  WebSocket:  WS   http://localhost:" + port + "/ws");
        System.out.println();
        System.out.println("示例请求:");
        System.out.println("  curl -X POST http://localhost:" + port + "/invocations \\");
        System.out.println("    -H 'Content-Type: application/json' \\");
        System.out.println("    -d '{\"message\": \"帮我计算 (100 + 200) * 3\"}'");
        System.out.println("-".repeat(60));

        app.run(port);
    }
}
