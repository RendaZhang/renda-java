package com.renda.leetcode.problems;

import com.renda.leetcode.core.LeetCodeProblem;

import java.util.*;

public class LC30_SubstringWithConcatenationOfAllWords implements LeetCodeProblem {

    public List<Integer> findSubstring(String s, String[] words) {
        List<Integer> ans = new ArrayList<>();
        if (s == null || words == null || words.length == 0) return ans;

        int n = s.length();
        int m = words.length;
        int L = words[0].length();
        if (L == 0) return ans;

        int total = m * L;
        if (n < total) return ans;

        // 1) 给每个唯一单词一个 id，并统计 needCount
        Map<String, Integer> idMap = new HashMap<>(m * 2);
        int uid = 0;
        for (String w : words) {
            if (!idMap.containsKey(w)) idMap.put(w, uid++);
        }
        int[] need = new int[uid];
        for (String w : words) need[idMap.get(w)]++;

        // 2) 对每个 offset 做滑动窗口
        for (int offset = 0; offset < L; offset++) {
            int left = offset;
            int count = 0;                 // 当前窗口中 token 的数量
            int[] window = new int[uid];   // 窗口内每个 id 的频次

            for (int right = offset; right + L <= n; right += L) {
                String token = s.substring(right, right + L);
                Integer id = idMap.get(token);

                if (id == null) {
                    // token 不在 words 中：窗口清空
                    Arrays.fill(window, 0);
                    count = 0;
                    left = right + L;
                    continue;
                }

                window[id]++;
                count++;

                // 如果这个词出现次数超过需要，就缩窗
                while (window[id] > need[id]) {
                    String leftToken = s.substring(left, left + L);
                    int leftId = idMap.get(leftToken); // 一定非 null
                    window[leftId]--;
                    count--;
                    left += L;
                }

                // token 数达到 m：记录起点，并继续滑动一格（保持窗口可继续找下一个）
                if (count == m) {
                    ans.add(left);
                    String leftToken = s.substring(left, left + L);
                    int leftId = idMap.get(leftToken);
                    window[leftId]--;
                    count--;
                    left += L;
                }
            }
        }

        return ans;
    }

    @Override
    public String problemNumber() {
        return "30";
    }

    @Override
    public void run() {
        System.out.println(findSubstring("aabbbbbbbcc", new String[]{"aa", "bb", "cc"}));
        System.out.println(findSubstring("barfoothefoobarman", new String[]{"foo","bar"}));
        System.out.println(findSubstring("wordgoodgoodgoodbestword", new String[]{"word","good","best","word"}));
        System.out.println(findSubstring("barfoofoobarthefoobarman", new String[]{"bar","foo","the"}));
        System.out.println(findSubstring("lingmindraboofooowingdingbarrwingmonkeypoundcake", new String[]{"fooo","barr","wing","ding","wing"}));
    }

}
