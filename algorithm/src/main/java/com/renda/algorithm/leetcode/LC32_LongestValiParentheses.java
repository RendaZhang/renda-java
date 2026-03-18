package com.renda.algorithm.leetcode;

import com.renda.algorithm.core.AlgorithmProblem;

import java.util.ArrayDeque;
import java.util.Deque;

public class LC32_LongestValiParentheses implements AlgorithmProblem {

    public int longestValidParentheses(String s) {
        char[] chars = s.toCharArray();
        int n = s.length();
        int ans = 0;
        Deque<Integer> stack = new ArrayDeque<>(n);
        stack.push(-1); // 哨兵，表示“最后一个无效位置”
        for (int i = 0; i < n; i++) {
            if (chars[i] == '(') {
                stack.push(i);
            } else {
                stack.pop();
                if (stack.isEmpty()) {
                    // 当前 ')' 没法匹配，成为新的无效边界
                    stack.push(i);
                } else {
                    // i 到 stack.peek() 之间是一个有效区间
                    ans = Math.max(ans, i - stack.peek());
                }
            }
        }
        return ans;
    }

    @Override
    public String getProblemId() {
        return "32";
    }

    @Override
    public void execute() {
        System.out.println(longestValidParentheses("(()"));
        System.out.println(longestValidParentheses(")()())"));
        System.out.println(longestValidParentheses("()(()"));
        System.out.println(longestValidParentheses("(()(()))"));
        System.out.println(longestValidParentheses("(()(())"));
    }
}
