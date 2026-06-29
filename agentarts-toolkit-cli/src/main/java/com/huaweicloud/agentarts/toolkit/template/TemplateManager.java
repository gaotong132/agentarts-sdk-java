package com.huaweicloud.agentarts.toolkit.template;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple template manager using {{ key }} placeholder substitution.
 *
 * <p>Mirrors Python TemplateManager from toolkit/utils/templates/manager.py.
 * Templates are loaded from classpath resources under /templates/.</p>
 */
public class TemplateManager {

    private static final String TEMPLATE_PREFIX = "/templates/";

    /**
     * Render a template to a file.
     *
     * @param templateName template path relative to /templates/ (e.g., "basic/Agent.java.tpl")
     * @param outputPath   destination file path
     * @param vars         key-value pairs for {{ key }} substitution (alternating key, value)
     */
    public static void renderToFile(String templateName, Path outputPath, String... vars) throws IOException {
        String content = loadTemplate(templateName);

        // Build variable map from alternating key-value pairs
        Map<String, String> varMap = new HashMap<>();
        for (int i = 0; i < vars.length - 1; i += 2) {
            varMap.put(vars[i], vars[i + 1] != null ? vars[i + 1] : "");
        }

        // Substitute {{ key }} placeholders
        for (Map.Entry<String, String> entry : varMap.entrySet()) {
            content = content.replace("{{ " + entry.getKey() + " }}", entry.getValue());
            content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, content);
    }

    /**
     * Load template content from classpath.
     */
    public static String loadTemplate(String templateName) throws IOException {
        String resourcePath = TEMPLATE_PREFIX + templateName;
        try (InputStream is = TemplateManager.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Template not found: " + resourcePath);
            }
            return new String(is.readAllBytes());
        }
    }

    /**
     * Render a template string with variable substitution.
     */
    public static String render(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{{ " + entry.getKey() + " }}", entry.getValue());
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
