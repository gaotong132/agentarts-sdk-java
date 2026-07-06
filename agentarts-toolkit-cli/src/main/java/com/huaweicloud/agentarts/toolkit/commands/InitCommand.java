package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.toolkit.operations.InitOperation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Initialize a new AgentArts project.
 *
 * <p>CLI command: initialize a new AgentArts project.</p>
 */
@Command(name = "init", description = "Initialize a new AgentArts project")
public class InitCommand implements Callable<Integer> {

    @Option(names = {"-n", "--name"}, description = "Project name")
    String name;

    @Option(names = {"-t", "--template"}, description = "Project template: basic, agentscope")
    String template;

    @Option(names = {"-p", "--path"}, description = "Project path", defaultValue = ".")
    String path;

    @Option(names = {"-r", "--region"}, description = "Huawei Cloud region")
    String region;

    @Option(names = "--swr-org", description = "SWR organization")
    String swrOrg;

    @Option(names = "--swr-repo", description = "SWR repository")
    String swrRepo;

    @Override
    public Integer call() {
        if (name == null || name.isEmpty()) {
            System.err.println("Error: --name is required");
            return 2;
        }
        // Validate name: lowercase letters, digits, hyphens
        String normalized = name.toLowerCase();
        if (!normalized.matches("[a-z0-9-]+")) {
            System.err.println("Error: name must contain only lowercase letters, digits, and hyphens");
            return 2;
        }
        name = normalized;
        if (template == null) template = "basic";
        if (region == null) region = "cn-southwest-2";
        if (swrRepo == null) swrRepo = name;

        try {
            InitOperation.initProject(template, name, path, region, swrOrg, swrRepo);
            System.out.println("Project '" + name + "' initialized successfully at " + path);
            return 0;
        } catch (Exception e) {
            System.err.println("Error initializing project: " + e.getMessage());
            return 1;
        }
    }
}
