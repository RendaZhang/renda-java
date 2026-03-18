package com.renda.algorithm.testsupport;

import com.renda.algorithm.core.AlgorithmProblem;
import com.renda.algorithm.core.ProblemSource;

public class BetaProblem implements AlgorithmProblem {
    @Override
    public String getProblemId() {
        return "B2";
    }

    @Override
    public ProblemSource getSource() {
        return ProblemSource.LEETCODE;
    }

    @Override
    public void execute() {
    }
}
