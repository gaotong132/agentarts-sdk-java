package com.huaweicloud.agentarts.sdk.memory.model;

/**
 * Retrieval configuration for memory search.
 * Mirrors Python RetrievalConfig with identical field names and defaults.
 */
public class RetrievalConfig {
    private String userId;
    private int maxTokens = 0;
    private int topK = 2;
    private double scoreThreshold = 0.6;

    public RetrievalConfig() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public double getScoreThreshold() { return scoreThreshold; }
    public void setScoreThreshold(double scoreThreshold) { this.scoreThreshold = scoreThreshold; }

    @Override
    public String toString() {
        return "RetrievalConfig{userId=" + userId + ", maxTokens=" + maxTokens
                + ", topK=" + topK + ", scoreThreshold=" + scoreThreshold + "}";
    }
}
