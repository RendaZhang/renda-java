package com.renda.algorithm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemRegistryTest {
    @Test
    void scansAndRegistersProblemsFromBasePackages() {
        ProblemRegistry registry = new ProblemRegistry(List.of("com.renda.algorithm.testsupport"));
        List<AlgorithmProblem> problems = registry.getProblems();

        assertThat(problems).hasSize(2);
        assertThat(problems).extracting(AlgorithmProblem::getProblemId).containsExactly("A1", "B2");
    }
}
