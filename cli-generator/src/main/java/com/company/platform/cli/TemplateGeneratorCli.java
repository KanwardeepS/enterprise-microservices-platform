package com.company.platform.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(name = "platform-template-generator", mixinStandardHelpOptions = true,
        description = "Copies service template and applies project-specific configuration.")
public class TemplateGeneratorCli implements Callable<Integer> {

    @Option(names = "--template-dir", description = "Path to service template directory")
    private Path templateDir = Path.of("service-template");

    @Option(names = "--output-dir", required = true, description = "Directory where generated service will be created")
    private Path outputDir;

    @Option(names = "--service-name", required = true, description = "Service name")
    private String serviceName;

    @Option(names = "--group-id", defaultValue = "com.company.services", description = "Maven groupId")
    private String groupId;

    @Option(names = "--artifact-id", description = "Maven artifactId (defaults to sanitized service name)")
    private String artifactId;

    @Option(names = "--package-name", description = "Java package name (defaults to groupId + service name)")
    private String packageName;

    @Option(names = "--event-provider", defaultValue = "kafka", description = "Event provider: kafka or rabbitmq")
    private String eventProvider;

    @Option(names = "--database", defaultValue = "oracle", description = "Database type: oracle or sybase")
    private String databaseType;

    @Option(names = "--security-enabled", defaultValue = "true", description = "Enable security starter")
    private boolean securityEnabled;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new TemplateGeneratorCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        validateInputs();
        String resolvedArtifactId = artifactId == null || artifactId.isBlank() ? sanitize(serviceName) : artifactId;
        String resolvedPackage = packageName == null || packageName.isBlank()
                ? groupId + "." + sanitize(serviceName).replace("-", "")
                : packageName;

        copyTemplate();
        replaceTemplateValues(resolvedArtifactId, resolvedPackage);
        rewriteTemplateConfiguration();
        moveJavaPackage(resolvedPackage);

        System.out.println("Template generated at: " + outputDir.toAbsolutePath());
        return 0;
    }

    private void validateInputs() {
        if (!Files.exists(templateDir) || !Files.isDirectory(templateDir)) {
            throw new CommandLine.ParameterException(new CommandLine(this),
                    "Template directory not found: " + templateDir.toAbsolutePath());
        }
        if (!eventProvider.equalsIgnoreCase("kafka") && !eventProvider.equalsIgnoreCase("rabbitmq")) {
            throw new CommandLine.ParameterException(new CommandLine(this),
                    "--event-provider must be either kafka or rabbitmq");
        }
        if (!databaseType.equalsIgnoreCase("oracle") && !databaseType.equalsIgnoreCase("sybase")) {
            throw new CommandLine.ParameterException(new CommandLine(this),
                    "--database must be either oracle or sybase");
        }
    }

    private void copyTemplate() throws IOException {
        Files.createDirectories(outputDir);
        Files.walkFileTree(templateDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = templateDir.relativize(dir);
                if (relative.startsWith("target")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Files.createDirectories(outputDir.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = templateDir.relativize(file);
                if (!relative.startsWith("target")) {
                    Files.copy(file, outputDir.resolve(relative), StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void replaceTemplateValues(String resolvedArtifactId, String resolvedPackage) throws IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("${SERVICE_NAME}", serviceName);
        replacements.put("${GROUP_ID}", groupId);
        replacements.put("${ARTIFACT_ID}", resolvedArtifactId);
        replacements.put("${PACKAGE_NAME}", resolvedPackage);
        replacements.put("sample-service", serviceName);
        replacements.put("service-template", resolvedArtifactId);
        replacements.put("com.company.service", resolvedPackage);

        Files.walkFileTree(outputDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isTextFile(file)) {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    for (Map.Entry<String, String> entry : replacements.entrySet()) {
                        content = content.replace(entry.getKey(), entry.getValue());
                    }
                    Files.writeString(file, content, StandardCharsets.UTF_8);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void rewriteTemplateConfiguration() throws IOException {
        Path applicationConfig = outputDir.resolve("src/main/resources/application.yml");
        if (!Files.exists(applicationConfig)) {
            return;
        }
        String config = Files.readString(applicationConfig, StandardCharsets.UTF_8);
        config = replaceYamlValue(config, "provider:", eventProvider.toLowerCase(Locale.ROOT));
        config = replaceYamlValue(config, "type:", databaseType.toLowerCase(Locale.ROOT));
        config = replaceYamlValue(config, "enabled:", String.valueOf(securityEnabled));
        Files.writeString(applicationConfig, config, StandardCharsets.UTF_8);
    }

    private String replaceYamlValue(String yaml, String key, String value) {
        return yaml.replaceAll("(?m)^([\\s-]*" + java.util.regex.Pattern.quote(key) + "\\s*).*$", "$1 " + value);
    }

    private void moveJavaPackage(String resolvedPackage) throws IOException {
        Path oldPackageDir = outputDir.resolve("src/main/java/com/company/service");
        if (!Files.exists(oldPackageDir)) {
            return;
        }
        Path newPackageDir = outputDir.resolve("src/main/java").resolve(resolvedPackage.replace('.', '/'));
        Files.createDirectories(newPackageDir);

        Files.walkFileTree(oldPackageDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = oldPackageDir.relativize(file);
                Path target = newPackageDir.resolve(relative);
                Files.createDirectories(target.getParent());
                Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });

        deleteIfEmpty(oldPackageDir);
        deleteIfEmpty(oldPackageDir.getParent());
        deleteIfEmpty(oldPackageDir.getParent().getParent());
    }

    private void deleteIfEmpty(Path directory) throws IOException {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.list(directory)) {
            if (paths.findAny().isEmpty()) {
                Files.delete(directory);
            }
        }
    }

    private boolean isTextFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".java")
                || fileName.endsWith(".xml")
                || fileName.endsWith(".yml")
                || fileName.endsWith(".yaml")
                || fileName.endsWith(".properties")
                || fileName.endsWith(".md")
                || fileName.endsWith(".dockerfile")
                || fileName.equals("dockerfile");
    }

    private String sanitize(String input) {
        return input.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
