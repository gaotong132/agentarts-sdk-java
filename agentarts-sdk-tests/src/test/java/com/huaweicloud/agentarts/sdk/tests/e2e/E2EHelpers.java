package com.huaweicloud.agentarts.sdk.tests.e2e;

/**
 * E2E test helpers — mirrors Python _helpers.py.
 */
public final class E2EHelpers {

    private E2EHelpers() {}

    /**
     * Build a unique resource name: {@code aa-it-{runId}-{kind}}, trimmed to maxLen.
     */
    public static String uniqueName(String kind, String runId) {
        return uniqueName(kind, runId, 40);
    }

    public static String uniqueName(String kind, String runId, int maxLen) {
        String name = "aa-it-" + runId + "-" + kind;
        if (name.length() > maxLen) {
            name = name.substring(0, maxLen);
        }
        // Strip trailing hyphens
        while (name.endsWith("-")) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

    /**
     * Run a cleanup action, swallowing errors.
     */
    public static void safeDelete(Runnable deleter, String description) {
        try {
            deleter.run();
        } catch (Exception e) {
            System.err.println("[safe-delete] " + description + ": " + e.getMessage());
        }
    }
}
