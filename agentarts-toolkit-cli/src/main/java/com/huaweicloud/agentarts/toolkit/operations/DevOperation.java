package com.huaweicloud.agentarts.toolkit.operations;

/**
 * Dev operation: run local development server.
 * Mirrors Python operations/runtime/dev.py.
 */
public class DevOperation {
    public static void runDevServer(int port, String host, boolean reload,
                                     String configPath, String[] envVars) throws Exception {
        // Apply environment variables from --env flags
        if (envVars != null) {
            for (String env : envVars) {
                int eq = env.indexOf('=');
                if (eq > 0) {
                    String key = env.substring(0, eq);
                    String value = env.substring(eq + 1);
                    System.out.println("Setting env: " + key + "=" + value);
                }
            }
        }
        System.out.println("Starting AgentArts dev server on " + host + ":" + port + "...");
        // TODO: instantiate AgentArtsRuntimeApp from config entrypoint and run
    }
}
