package com.renda.algorithm.testsupport;

import com.renda.algorithm.core.AlgorithmProblem;
import com.renda.algorithm.core.ProblemSource;

public class AlphaProblem implements AlgorithmProblem {
    @Override
    public String getProblemId() {
        return "A1";
    }

    @Override
    public ProblemSource getSource() {
        return ProblemSource.LEETCODE;
    }

    @Override
    public void execute() {
    }
}
