package com.renda.algorithm.leetcode;

import com.renda.algorithm.core.AlgorithmProblem;

public class LC32_LongestValiParentheses implements AlgorithmProblem {

    public int longestValidParentheses(String s) {
        return s.length();
    }

    @Override
    public String getProblemId() {
        return "32";
    }

    @Override
    public void execute() {
        System.out.println(longestValidParentheses("(()"));
        System.out.println(longestValidParentheses(")()())"));
        System.out.println(longestValidParentheses(""));
    }
}
