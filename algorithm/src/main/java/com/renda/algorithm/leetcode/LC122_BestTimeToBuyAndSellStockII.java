package com.renda.algorithm.leetcode;

import com.renda.algorithm.core.*;

/**
 * LeetCode 122 - Best Time to Buy and Sell Stock II.
 */
public class LC122_BestTimeToBuyAndSellStockII implements AlgorithmProblem {

    @Override
    public ProblemSource getSource() {
        return ProblemSource.LEETCODE;
    }

    public int maxProfit(int[] prices) {
        int n = prices.length;
        int profit = 0;
        int tmp = prices[0];
        for (int i = 1; i < n; i++) {
            if (prices[i] > tmp) profit += prices[i] - tmp;
            tmp = prices[i];
        }
        return profit;
    }

    @Override
    public String getProblemId() {
        return "122";
    }

    @Override
    public void execute() {
        System.out.println("LC122 Best Time to Buy and Sell Stock II: " + maxProfit(new int[]{7,1,5,3,6,4}));
    }
}
