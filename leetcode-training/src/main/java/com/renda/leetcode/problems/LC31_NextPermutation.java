package com.renda.leetcode.problems;

import com.renda.leetcode.core.LeetCodeProblem;

import java.util.Arrays;

public class LC31_NextPermutation implements LeetCodeProblem {

    public void nextPermutation(int[] nums) {
        int n = nums.length;
        if (n <= 1) return;

        // 1) find pivot: the first i from right where nums[i] < nums[i+1]
        int i = n - 2;
        while (i >= 0 && nums[i] >= nums[i + 1]) i--;

        // 2) if no pivot, reverse whole array
        if (i < 0) {
            reverse(nums, 0, n - 1);
            return;
        }

        // 3) find rightmost element > nums[i], swap
        int j = n - 1;
        while (nums[j] <= nums[i]) j--;
        swap(nums, i, j);

        // 4) reverse suffix to get the smallest order
        reverse(nums, i + 1, n - 1);
    }

    private void reverse(int[] a, int l, int r) {
        while (l < r) swap(a, l++, r--);
    }

    private void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    @Override
    public String problemNumber() {
        return "31";
    }

    @Override
    public void run() {
        int[] nums1 = {5, 1, 1};
        nextPermutation(nums1);
        System.out.println(Arrays.toString(nums1));
    }
}
