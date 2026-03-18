package com.renda.algorithm.core;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class ClasspathProblemScanner {
    private final ClassLoader classLoader;

    public ClasspathProblemScanner(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    public List<Class<? extends AlgorithmProblem>> scan(String basePackage) {
        if (basePackage == null || basePackage.isBlank()) {
            throw new IllegalArgumentException("basePackage is blank");
        }

        String packagePath = basePackage.replace('.', '/');
        List<Class<? extends AlgorithmProblem>> results = new ArrayList<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    scanFileSystem(results, basePackage, url);
                    continue;
                }
                if ("jar".equals(protocol)) {
                    scanJar(results, packagePath, url);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan package: " + basePackage, e);
        }

        return results;
    }

    private void scanFileSystem(List<Class<? extends AlgorithmProblem>> results, String basePackage, URL url) {
        Path root;
        try {
            root = Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid URL: " + url, e);
        }

        if (!Files.exists(root)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(root)) {
            stream
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".class"))
                .forEach(p -> {
                    String className = toClassName(basePackage, root, p);
                    tryAdd(results, className);
                });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan filesystem for: " + basePackage, e);
        }
    }

    private void scanJar(List<Class<? extends AlgorithmProblem>> results, String packagePath, URL url) {
        try {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(packagePath)) {
                        continue;
                    }
                    if (!name.endsWith(".class")) {
                        continue;
                    }
                    if (name.contains("$")) {
                        continue;
                    }
                    String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                    tryAdd(results, className);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan jar for: " + packagePath, e);
        }
    }

    private String toClassName(String basePackage, Path root, Path classFile) {
        Path relative = root.relativize(classFile);
        String rel = relative.toString().replace(File.separatorChar, '.');
        rel = rel.substring(0, rel.length() - ".class".length());
        if (rel.isBlank()) {
            return basePackage;
        }
        return basePackage + "." + rel;
    }

    @SuppressWarnings("unchecked")
    private void tryAdd(List<Class<? extends AlgorithmProblem>> results, String className) {
        if (className.contains("$")) {
            return;
        }
        try {
            Class<?> raw = Class.forName(className, false, classLoader);
            if (!AlgorithmProblem.class.isAssignableFrom(raw)) {
                return;
            }
            if (raw.isInterface()) {
                return;
            }
            if (Modifier.isAbstract(raw.getModifiers())) {
                return;
            }
            if (raw.isAnonymousClass() || raw.isLocalClass() || raw.isMemberClass()) {
                return;
            }
            results.add((Class<? extends AlgorithmProblem>) raw);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class not found: " + className, e);
        } catch (LinkageError e) {
            throw new IllegalStateException("Failed to load class: " + className, e);
        }
    }
}
