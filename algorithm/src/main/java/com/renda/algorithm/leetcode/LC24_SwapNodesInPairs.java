package com.renda.algorithm.leetcode;

import com.renda.algorithm.core.*;
import com.renda.algorithm.core.ListNode;
import com.renda.algorithm.core.ListUtils;

public class LC24_SwapNodesInPairs implements AlgorithmProblem {

    @Override
    public ProblemSource getSource() {
        return ProblemSource.LEETCODE;
    }

    public ListNode swapPairs(ListNode head) {
        if (head == null || head.next == null) return head;
        ListNode dummyNode = new ListNode(0, head);
        ListNode prevNode = dummyNode;
        ListNode p1 = head;
        ListNode p2 = head.next;
        while (p1 != null && p2 != null) {
            prevNode.next = p2;
            p1.next = p2.next;
            p2.next = p1;
            prevNode = p1;
            p1 = prevNode.next;
            if (p1 != null) p2 = p1.next;
            else p2 = null;
        }
        return dummyNode.next;
    }

    @Override
    public String getProblemId() {
        return "24";
    }

    @Override
    public void execute() {
        System.out.println(swapPairs(ListUtils.buildList(new int[]{1,2,3,4})));
        System.out.println(swapPairs(ListUtils.buildList(new int[]{})));
        System.out.println(swapPairs(ListUtils.buildList(new int[]{1})));
        System.out.println(swapPairs(ListUtils.buildList(new int[]{1,2,3})));
    }

}
