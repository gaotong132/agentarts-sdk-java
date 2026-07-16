package com.huaweicloud.agentarts.sdk.identity;

import com.huaweicloud.agentarts.sdk.identity.auth.TokenPoller;
import com.huaweicloud.agentarts.sdk.identity.config.LocalIdentityConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for identity module: LocalIdentityConfig, TokenPoller.
 */
class IdentityModuleTest {

    // ========================
    // LocalIdentityConfig
    // ========================

    @Nested
    @DisplayName("LocalIdentityConfig")
    class ConfigTests {

        @TempDir
        Path tempDir;

        @Test
        void shouldReturnEmptyConfigWhenFileMissing() {
            LocalIdentityConfig config = LocalIdentityConfig.load(
                    tempDir.resolve("nonexistent.json").toString());
            assertNull(config.getWorkloadIdentityName());
            assertNull(config.getUserId());
        }

        @Test
        void shouldSaveAndLoad() {
            String path = tempDir.resolve("test_identity.json").toString();

            LocalIdentityConfig config = new LocalIdentityConfig();
            config.setPath(path);
            config.setWorkloadIdentityName("test-workload");
            config.setUserId("user-123");
            config.save();

            assertTrue(new File(path).exists());

            LocalIdentityConfig loaded = LocalIdentityConfig.load(path);
            assertEquals("test-workload", loaded.getWorkloadIdentityName());
            assertEquals("user-123", loaded.getUserId());
            assertFalse(assertDoesNotThrow(() -> Files.readString(Path.of(path)))
                    .contains("\"path\""), "the local filesystem path is runtime metadata");
        }

        @Test
        void malformedConfigFailsClosedAndIsNotOverwritten() throws Exception {
            Path path = tempDir.resolve("malformed.json");
            String malformed = "{not-json";
            Files.writeString(path, malformed, StandardCharsets.UTF_8);

            assertThrows(IllegalStateException.class,
                    () -> LocalIdentityConfig.load(path.toString()));
            assertEquals(malformed, Files.readString(path, StandardCharsets.UTF_8));
        }

        @Test
        void atomicSaveLeavesNoTemporaryFiles() throws Exception {
            Path path = tempDir.resolve("nested/identity.json");
            LocalIdentityConfig config = new LocalIdentityConfig();
            config.setPath(path.toString());
            config.setUserId("user");

            config.save();

            assertTrue(Files.isRegularFile(path));
            try (var leftovers = Files.newDirectoryStream(path.getParent(), ".agent-identity-*.tmp")) {
                assertFalse(leftovers.iterator().hasNext());
            }
        }

        @Test
        void shouldHaveDefaultPath() {
            LocalIdentityConfig config = new LocalIdentityConfig();
            assertEquals(".agent_identity.json", config.getPath());
        }
    }

    // ========================
    // TokenPoller
    // ========================

    @Nested
    @DisplayName("TokenPoller")
    class PollerTests {

        @Test
        void shouldReturnCompletedToken() {
            TokenPoller poller = new TokenPoller() {
                int count = 0;
                @Override
                public PollResult poll() {
                    count++;
                    if (count < 3) return PollResult.inProgress();
                    return PollResult.completed("my-token-123");
                }
            }.withInterval(0).withTimeout(5); // 0ms interval for fast test

            String token = poller.waitForToken();
            assertEquals("my-token-123", token);
        }

        @Test
        void shouldThrowOnFailed() {
            TokenPoller poller = new TokenPoller() {
                @Override
                public PollResult poll() {
                    return PollResult.failed("Auth failed");
                }
            }.withInterval(0).withTimeout(5);

            RuntimeException ex = assertThrows(RuntimeException.class, poller::waitForToken);
            assertTrue(ex.getMessage().contains("Auth failed"));
        }

        @Test
        void shouldThrowOnTimeout() {
            TokenPoller poller = new TokenPoller() {
                @Override
                public PollResult poll() {
                    return PollResult.inProgress();
                }
            }.withInterval(0).withTimeout(1); // 1 second timeout

            assertThrows(RuntimeException.class, poller::waitForToken);
        }

        @Test
        void pollResultFactoryMethods() {
            var inProgress = TokenPoller.PollResult.inProgress();
            assertEquals(TokenPoller.PollStatus.IN_PROGRESS, inProgress.status());
            assertNull(inProgress.token());

            var completed = TokenPoller.PollResult.completed("tok");
            assertEquals(TokenPoller.PollStatus.COMPLETED, completed.status());
            assertEquals("tok", completed.token());

            var failed = TokenPoller.PollResult.failed("err");
            assertEquals(TokenPoller.PollStatus.FAILED, failed.status());
            assertEquals("err", failed.error());
        }
    }
}
