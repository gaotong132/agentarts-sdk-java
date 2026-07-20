package com.huaweicloud.agentarts.examples.sandbox;

import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.filesystem.spec.SandboxFilesystemSpec;
import io.agentscope.harness.agent.sandbox.SandboxClient;
import io.agentscope.harness.agent.sandbox.SandboxClientOptions;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import io.agentscope.harness.agent.sandbox.snapshot.NoopSnapshotSpec;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec;

public class AgentArtsBasedSandboxFilesystemSpec extends SandboxFilesystemSpec {
    private final String baseUrl;

    private final String apiKey;

    private final AgentArtsSandboxClientOption options;

    public AgentArtsBasedSandboxFilesystemSpec(String baseUrl, String apiKey) {
        super();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;

        options = new AgentArtsSandboxClientOption();
        options.setBaseUrl(baseUrl);
        options.setApiKey(apiKey);
        isolationScope(IsolationScope.GLOBAL);
    }

    @Override
    protected SandboxClient<?> createClient() {
        return options.createClient();
    }

    @Override
    protected SandboxClientOptions clientOptions() {
        return options;
    }

    @Override
    protected SandboxSnapshotSpec snapshotSpec() {
        return new NoopSnapshotSpec();
    }

    @Override
    protected WorkspaceSpec workspaceSpec() {
        WorkspaceSpec spec = new WorkspaceSpec();
        spec.setRoot("/workspace");
        return spec;
    }
}
