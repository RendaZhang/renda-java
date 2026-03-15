package com.renda.algorithm.core;

import java.util.List;

/**
 * 题目执行策略接口
 */
public interface ProblemStrategy {
    void runProblem(AlgorithmProblem problem);
    void runAll(List<AlgorithmProblem> problems);
}
