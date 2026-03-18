package com.renda.algorithm.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class ProblemRegistry implements ProblemFactory {
    private final List<AlgorithmProblem> problems;

    public ProblemRegistry() {
        this(List.of(
            "com.renda.algorithm.leetcode",
            "com.renda.algorithm.luogu",
            "com.renda.algorithm.swordoffer"
        ));
    }

    public ProblemRegistry(List<String> basePackages) {
        Objects.requireNonNull(basePackages, "basePackages");
        this.problems = new ArrayList<>();

        ClasspathProblemScanner scanner = new ClasspathProblemScanner(Thread.currentThread().getContextClassLoader());
        Map<String, AlgorithmProblem> unique = new TreeMap<>();

        for (String basePackage : basePackages) {
            for (Class<? extends AlgorithmProblem> clazz : scanner.scan(basePackage)) {
                AlgorithmProblem instance = instantiate(clazz);
                String key = instance.getSource() + ":" + instance.getProblemId();
                AlgorithmProblem existing = unique.putIfAbsent(key, instance);
                if (existing != null) {
                    throw new IllegalStateException("Duplicate problem key: " + key);
                }
            }
        }

        this.problems.addAll(unique.values());
        this.problems.sort(problemComparator());
    }

    @Override
    public List<AlgorithmProblem> getProblems() {
        return problems;
    }

    private static AlgorithmProblem instantiate(Class<? extends AlgorithmProblem> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate: " + clazz.getName(), e);
        }
    }

    private static Comparator<AlgorithmProblem> problemComparator() {
        return Comparator
            .comparing((AlgorithmProblem p) -> p.getSource().ordinal())
            .thenComparingInt(ProblemRegistry::extractNumber)
            .thenComparing(AlgorithmProblem::getProblemId);
    }

    private static int extractNumber(AlgorithmProblem p) {
        String id = p.getProblemId();
        if (id == null) {
            return Integer.MAX_VALUE;
        }
        String digits = id.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
