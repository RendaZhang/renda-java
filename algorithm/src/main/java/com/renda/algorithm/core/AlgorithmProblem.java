package com.renda.algorithm.core;

/**
 * 通用算法题目接口
 */
public interface AlgorithmProblem {
    /**
     * 获取题目编号/ID
     */
    String getProblemId();

    /**
     * 获取题目来源 (LeetCode, Luogu, SwordOffer)
     */
    ProblemSource getSource();

    /**
     * 执行题目示例/测试逻辑
     */
    void execute();

    /**
     * 时间复杂度说明
     */
    default String getTimeComplexity() {
        return "Not specified";
    }

    /**
     * 空间复杂度说明
     */
    default String getSpaceComplexity() {
        return "Not specified";
    }
}
