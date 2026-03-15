package com.renda.algorithm.leetcode;

import com.renda.algorithm.core.*;

public class LC29_DivideTwoIntegers implements AlgorithmProblem {

    @Override
    public ProblemSource getSource() {
        return ProblemSource.LEETCODE;
    }

    public int divide(int dividend, int divisor) {
        if (divisor == 0) throw new ArithmeticException("Divided by Zero");
        else if (divisor == 1) return dividend;
        else if (divisor == -1) return dividend == Integer.MIN_VALUE ? Integer.MAX_VALUE : -dividend;
        long a = Math.abs((long) dividend),  b = Math.abs((long) divisor);
        long r = 0;
        while (a >= b) {
            long multiple = 1;
            long product = b;
            long temp = b << 1;
            while (temp <= a) {
                product = temp;
                multiple <<= 1;
                temp <<= 1;
            }
            a -= product;
            r += multiple;
        }
        return (int) ((dividend < 0) ^ (divisor < 0) ? -r : r);
    }

    @Override
    public String getProblemId() {
        return "29";
    }

    @Override
    public void execute() {
        System.out.println(divide(10, 3));
        System.out.println(divide(7, -3));
        System.out.println(divide(8, 2));
        System.out.println(divide(Integer.MIN_VALUE, -1));
        System.out.println(divide(Integer.MAX_VALUE, -1));
    }

}
