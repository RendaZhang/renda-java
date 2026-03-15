package com.renda.algorithm.leetcode;

import com.renda.algorithm.core.*;
import com.renda.algorithm.core.ListNode;
import com.renda.algorithm.core.ListUtils;

public class LC19_RemoveNthNodeFromEndOfList implements AlgorithmProblem {

    @Override
    public ProblemSource getSource() {
        return ProblemSource.LEETCODE;
    }

    public ListNode removeNthFromEnd(ListNode head, int n) {
        if (head == null) return null;
        ListNode tempNode = head;
        int count = 0;
        while (tempNode != null) {
            count++;
            tempNode = tempNode.next;
        }
        int m = count - n;
        if (m <= 0) return head.next;
        ListNode currNode = head;
        ListNode prevNode = currNode;
        for (int i = 0; i < m; i++) {
            prevNode = currNode;
            currNode = currNode.next;
        }
        prevNode.next = currNode.next;
        return head;
    }

    @Override
    public String getProblemId() {
        return "19";
    }

    @Override
    public void execute() {
        System.out.println(removeNthFromEnd(ListUtils.buildList(new int[]{1, 2, 3, 4, 5}), 2));
        System.out.println(removeNthFromEnd(ListUtils.buildList(new int[]{1}), 1));
        System.out.println(removeNthFromEnd(ListUtils.buildList(new int[]{1, 2}), 1));
    }
}
