package com.huaweicloud.agentarts.toolkit.operations;

import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;

import java.util.concurrent.CountDownLatch;

/** Internal child-JVM entrypoint used by {@link DevOperation}. */
public final class DevServerLauncher {

    private DevServerLauncher() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Expected arguments: <host> <port> <entrypoint> <project>");
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        DevOperation.validateAddress(port, host);

        DevOperation.LoadedApp loaded = DevOperation.loadApp(args[2], args[3]);
        AgentArtsRuntimeApp app = loaded.app();
        CountDownLatch stop = new CountDownLatch(1);
        Thread shutdownHook = new Thread(stop::countDown, "agentarts-dev-child-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            app.run(port, host);
            System.out.println("DEV_SERVER_LISTENING on " + host + ":" + app.getPort());
            stop.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM shutdown is already in progress.
            }
            try {
                app.stop();
            } finally {
                DevOperation.closeLoader(loaded.loader());
            }
        }
    }
}
