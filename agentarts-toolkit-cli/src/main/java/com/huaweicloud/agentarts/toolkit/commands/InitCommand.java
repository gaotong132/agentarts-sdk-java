package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.toolkit.operations.InitOperation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Initialize a new AgentArts project.
 *
 * <p>Mirrors Python {@code init} command from {@code cli/runtime/init.py}.</p>
 */
@Command(name = "init", description = "Initialize a new AgentArts project")
public class InitCommand implements Runnable {

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
    public void run() {
        if (name == null || name.isEmpty()) {
            System.err.println("Error: --name is required");
            return;
        }
        // Validate name: lowercase letters, digits, hyphens
        name = name.toLowerCase();
        if (!name.matches("[a-z0-9-]+")) {
            System.err.println("Error: name must contain only lowercase letters, digits, and hyphens");
            return;
        }
        if (template == null) template = "basic";
        if (region == null) region = "cn-southwest-2";
        if (swrRepo == null) swrRepo = name;

        try {
            InitOperation.initProject(template, name, path, region, swrOrg, swrRepo);
            System.out.println("Project '" + name + "' initialized successfully at " + path);
        } catch (Exception e) {
            System.err.println("Error initializing project: " + e.getMessage());
        }
    }
}
