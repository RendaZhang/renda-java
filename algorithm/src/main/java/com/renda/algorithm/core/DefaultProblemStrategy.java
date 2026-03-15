package com.renda.algorithm.core;

import java.util.List;

public class DefaultProblemStrategy implements ProblemStrategy {
    @Override
    public void runProblem(AlgorithmProblem problem) {
        System.out.println("========================================");
        System.out.println("Running [" + problem.getSource() + "] Problem: " + problem.getProblemId());
        System.out.println("Time Complexity: " + problem.getTimeComplexity());
        System.out.println("Space Complexity: " + problem.getSpaceComplexity());
        System.out.println("----------------------------------------");
        problem.execute();
        System.out.println("========================================\n");
    }

    @Override
    public void runAll(List<AlgorithmProblem> problems) {
        problems.forEach(this::runProblem);
    }
}
