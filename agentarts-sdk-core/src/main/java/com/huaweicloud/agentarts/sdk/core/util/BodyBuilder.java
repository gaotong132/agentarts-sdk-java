package com.huaweicloud.agentarts.sdk.core.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流式构建请求 Body（Map），自动跳过 null 和空字符串值。
 *
 * <p>替代大量 {@code if (x != null) body.put("key", x)} 重复代码。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * Map<String, Object> body = BodyBuilder.create()
 *     .put("description", description)          // null 时自动跳过
 *     .putIfNotBlank("name", name)               // 空白字符串时跳过
 *     .put("artifact_source", artifactConfig)    // null 时跳过
 *     .put("tags", tagsList)                     // null 时跳过
 *     .build();
 * }</pre>
 */
public final class BodyBuilder {

    private final Map<String, Object> body = new LinkedHashMap<>();

    private BodyBuilder() {}

    /** 创建新的 BodyBuilder。 */
    public static BodyBuilder create() {
        return new BodyBuilder();
    }

    /**
     * 添加键值对，值为 null 时自动跳过。
     *
     * @param key   键名
     * @param value 值（null 时不添加）
     * @return this
     */
    public BodyBuilder put(String key, Object value) {
        if (value != null) {
            body.put(key, value);
        }
        return this;
    }

    /**
     * 添加字符串键值对，值为 null 或空白时跳过。
     *
     * @param key   键名
     * @param value 字符串值（null 或空白时不添加）
     * @return this
     */
    public BodyBuilder putIfNotBlank(String key, String value) {
        if (JsonUtils.isNotBlank(value)) {
            body.put(key, value);
        }
        return this;
    }

    /**
     * 添加列表键值对，列表为 null 或空时跳过。
     *
     * @param key   键名
     * @param value 列表值（null 或空时不添加）
     * @return this
     */
    public BodyBuilder putIfNotEmpty(String key, List<?> value) {
        if (value != null && !value.isEmpty()) {
            body.put(key, value);
        }
        return this;
    }

    /**
     * 添加 Map 键值对，Map 为 null 或空时跳过。
     *
     * @param key   键名
     * @param value Map 值（null 或空时不添加）
     * @return this
     */
    public BodyBuilder putIfNotEmpty(String key, Map<?, ?> value) {
        if (value != null && !value.isEmpty()) {
            body.put(key, value);
        }
        return this;
    }

    /**
     * 添加 boolean 值（始终添加，用于显式 false 的场景）。
     *
     * @param key   键名
     * @param value boolean 值
     * @return this
     */
    public BodyBuilder put(String key, boolean value) {
        body.put(key, value);
        return this;
    }

    /**
     * 添加 int 值（始终添加）。
     *
     * @param key   键名
     * @param value int 值
     * @return this
     */
    public BodyBuilder put(String key, int value) {
        body.put(key, value);
        return this;
    }

    /**
     * 添加 long 值（始终添加）。
     *
     * @param key   键名
     * @param value long 值
     * @return this
     */
    public BodyBuilder put(String key, long value) {
        body.put(key, value);
        return this;
    }

    /** 构建最终的 Map。 */
    public Map<String, Object> build() {
        return body;
    }
}
