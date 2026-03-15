package com.renda.algorithm.core;

import com.renda.algorithm.leetcode.*;
import java.util.ArrayList;
import java.util.List;

public class ProblemRegistry implements ProblemFactory {
    private final List<AlgorithmProblem> problems = new ArrayList<>();

    public ProblemRegistry() {
        // LeetCode Problems
        problems.add(new LC1_TwoSum());
        problems.add(new LC322_CoinChange());
        problems.add(new LC15_3Sum());
        problems.add(new LC703_KthLargestElementInAStream());
    }

    @Override
    public List<AlgorithmProblem> getProblems() {
        return problems;
    }
}
