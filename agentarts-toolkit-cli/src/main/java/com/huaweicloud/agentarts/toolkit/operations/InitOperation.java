package com.huaweicloud.agentarts.toolkit.operations;

import com.huaweicloud.agentarts.toolkit.template.TemplateManager;

import java.io.IOException;
import java.nio.file.*;

/**
 * Init operation: creates project skeleton from templates.
 *
 * <p>Operation: initialize a new project from templates.</p>
 */
public final class InitOperation {

    private static final java.util.Set<String> SUPPORTED_TEMPLATES =
            java.util.Set.of("basic", "agentscope");

    private InitOperation() {
    }

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
        if (!SUPPORTED_TEMPLATES.contains(template)) {
            throw new IllegalArgumentException("Unsupported template: " + template);
        }
        if (name == null || !name.matches("[a-z0-9][a-z0-9-]{0,62}")) {
            throw new IllegalArgumentException(
                    "name must be 1-63 lowercase letters, digits, or hyphens and start with a letter or digit");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("region must not be blank");
        }
        Path root = Paths.get(path).toAbsolutePath().normalize();
        Path projectDir = root.resolve(name).normalize();
        if (!projectDir.startsWith(root)) {
            throw new IllegalArgumentException("project path escapes the requested root");
        }
        Files.createDirectories(root);
        if (Files.exists(projectDir)) {
            throw new FileAlreadyExistsException(
                    "Refusing to overwrite existing project directory: " + projectDir);
        }
        Path staging = root.resolve("." + name + ".agentarts-init-"
                + java.util.UUID.randomUUID()).normalize();
        try {
            Path srcDir = staging.resolve("src/main/java/com/example");
            Files.createDirectories(srcDir);
            TemplateManager.renderToFile(
                    template + "/pom.xml.tpl", staging.resolve("pom.xml"),
                    "name", name, "region", region);
            TemplateManager.renderToFile(
                    template + "/Agent.java.tpl", srcDir.resolve("Agent.java"),
                    "name", name);
            TemplateManager.renderToFile(
                    "basic/config.yaml.tpl", staging.resolve(".agentarts_config.yaml"),
                    "name", name, "region", region,
                    "swr_org", swrOrg != null ? swrOrg : name,
                    "swr_repo", swrRepo != null ? swrRepo : name);
            TemplateManager.renderToFile(
                    "docker/Dockerfile.tpl", staging.resolve("Dockerfile"),
                    "name", name);
            moveIntoPlace(staging, projectDir);
        } catch (IOException | RuntimeException e) {
            try {
                deleteStagingDirectory(staging);
            } catch (IOException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw e;
        }
    }

    private static void moveIntoPlace(Path staging, Path projectDir) throws IOException {
        try {
            Files.move(staging, projectDir, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(staging, projectDir);
        }
    }

    private static void deleteStagingDirectory(Path staging) throws IOException {
        if (!Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        Files.walkFileTree(staging, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(
                        Path file, java.nio.file.attribute.BasicFileAttributes attrs)
                        throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException error)
                        throws IOException {
                    if (error != null) {
                        throw error;
                    }
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
    }
}
