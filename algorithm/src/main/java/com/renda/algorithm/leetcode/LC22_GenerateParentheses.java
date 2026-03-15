package com.renda.algorithm.leetcode;

import com.renda.algorithm.core.*;

import java.util.ArrayList;
import java.util.List;

public class LC22_GenerateParentheses implements AlgorithmProblem {

    @Override
    public ProblemSource getSource() {
        return ProblemSource.LEETCODE;
    }

    public List<String> generateParenthesis(int n) {
        List<String> result = new ArrayList<>();
        recursiveGenerateParenthesis(0, 0, n, "", result);
        return result;
    }

    void recursiveGenerateParenthesis(int x, int y, int n, String s, List<String> result) {
        if (x > n || y > n || x < y) return;
        if (x == y && x == n) result.add(s);
        recursiveGenerateParenthesis(x + 1, y, n, s + '(', result);
        recursiveGenerateParenthesis(x, y + 1, n, s + ')', result);
    }

    @Override
    public String getProblemId() {
        return "22";
    }

    @Override
    public void execute() {
        System.out.println(generateParenthesis(3));
        System.out.println(generateParenthesis(1));
    }

}
