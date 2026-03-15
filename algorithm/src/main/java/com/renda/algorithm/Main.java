package com.renda.algorithm;

import com.renda.algorithm.core.*;

/**
 * 算法实验室统一启动入口 (Strategy Pattern + Abstract Factory)
 */
public class Main {
    public static void main(String[] args) {
        // 1. 获取工厂 (此处可根据参数切换 LeetCode/Luogu 工厂)
        ProblemFactory factory = new ProblemRegistry();

        // 2. 设置执行策略
        ProblemStrategy strategy = new DefaultProblemStrategy();

        // 3. 执行
        System.out.println("Starting Algorithm Laboratory...");
        strategy.runAll(factory.getProblems());
    }
}
