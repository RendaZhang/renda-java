package com.renda.algorithm.core;

import java.util.List;

/**
 * 抽象工厂：用于创建/获取题目集
 */
public interface ProblemFactory {
    List<AlgorithmProblem> getProblems();
}
