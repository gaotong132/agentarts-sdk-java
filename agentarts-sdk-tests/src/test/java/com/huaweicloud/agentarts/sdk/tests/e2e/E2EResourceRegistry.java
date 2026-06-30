package com.huaweicloud.agentarts.sdk.tests.e2e;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LIFO resource cleanup registry — mirrors Python _ResourceRegistry.
 *
 * <p>Every resource created by a test is registered here; at session end
 * {@link #cleanupAll()} calls each deleter in reverse order, swallowing errors
 * so a failing cleanup never masks a real failure.</p>
 */
public class E2EResourceRegistry {

    private final List<Entry> entries = Collections.synchronizedList(new ArrayList<>());

    /**
     * Register a cleanup action.
     *
     * @param deleter runnable that deletes the resource
     * @param description human-readable description for logging
     */
    public void register(Runnable deleter, String description) {
        entries.add(new Entry(deleter, description));
    }

    /**
     * Drain the registry in LIFO order, swallowing errors.
     */
    public void cleanupAll() {
        List<Entry> reversed = new ArrayList<>(entries);
        Collections.reverse(reversed);
        for (Entry entry : reversed) {
            try {
                entry.deleter.run();
                System.out.println("[cleanup] Deleted: " + entry.description);
            } catch (Exception e) {
                System.err.println("[cleanup] Failed to delete " + entry.description + ": " + e.getMessage());
            }
        }
        entries.clear();
    }

    private record Entry(Runnable deleter, String description) {}
}
