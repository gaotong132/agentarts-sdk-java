package com.huaweicloud.agentarts.toolkit.operations;

import com.huaweicloud.agentarts.toolkit.template.TemplateManager;

import java.io.IOException;
import java.nio.file.*;

/**
 * Init operation: creates project skeleton from templates.
 *
 * <p>Operation: initialize a new project from templates.</p>
 */
public class InitOperation {

    /**
     * Initialize a new AgentArts project.
     *
     * @param template template name (basic, agentscope)
     * @param name     project name
     * @param path     project directory
     * @param region   Huawei Cloud region
     * @param swrOrg   SWR organization (nullable)
     * @param swrRepo  SWR repository (nullable, defaults to name)
     */
    public static void initProject(String template, String name, String path,
                                    String region, String swrOrg, String swrRepo) throws IOException {
        Path projectDir = Paths.get(path, name);
        Files.createDirectories(projectDir);

        // Create src directory
        Path srcDir = projectDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        // Generate pom.xml
        TemplateManager.renderToFile("basic/pom.xml.tpl", projectDir.resolve("pom.xml"),
                "name", name, "region", region);

        // Generate agent source file
        String templateFile = template + "/Agent.java.tpl";
        TemplateManager.renderToFile(templateFile, srcDir.resolve("Agent.java"),
                "name", name);

        // Generate .agentarts_config.yaml
        TemplateManager.renderToFile("basic/config.yaml.tpl",
                projectDir.resolve(".agentarts_config.yaml"),
                "name", name, "region", region,
                "swr_org", swrOrg != null ? swrOrg : name,
                "swr_repo", swrRepo != null ? swrRepo : name);

        // Generate Dockerfile
        TemplateManager.renderToFile("docker/Dockerfile.tpl",
                projectDir.resolve("Dockerfile"),
                "name", name);
    }
}
