package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.toolkit.operations.InitOperation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Initialize a new AgentArts project.
 *
 * <p>CLI command: initialize a new AgentArts project. When {@code --name} is
 * omitted and a console is attached, the command prompts interactively for the
 * project name (all other options default sensibly); in a non-interactive
 * context it errors with exit code 2.</p>
 */
@Command(name = "init", mixinStandardHelpOptions = true,
        description = "Initialize a new AgentArts project")
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
        // --name is the only field without a default. When it is omitted the
        // command is "interactive init": prompt on the console for the project
        // name (matching the documented `agentarts init` flow). If no console
        // is attached (piped input, CI, IDE test harness), fall back to a hard
        // error so the caller still gets a non-zero exit and no half-created
        // project.
        if (name == null || name.isEmpty()) {
            java.io.Console console = System.console();
            if (console == null) {
                System.err.println("Error: --name is required (no interactive console; pass -n <name>)");
                return 2;
            }
            name = console.readLine("Project name: ");
            if (name == null) { // Ctrl-D / EOF
                System.err.println("Error: --name is required");
                return 2;
            }
            name = name.trim();
            if (name.isEmpty()) {
                System.err.println("Error: --name is required");
                return 2;
            }
        }
        // Validate name: lowercase letters, digits, hyphens
        String normalized = name.toLowerCase();
        if (!normalized.matches("[a-z0-9][a-z0-9-]{0,62}")) {
            System.err.println("Error: name must be 1-63 lowercase letters, digits, or hyphens "
                    + "and start with a letter or digit");
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
