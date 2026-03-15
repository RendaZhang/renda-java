package com.renda.algorithm.leetcode;

import com.renda.algorithm.core.*;
import com.renda.algorithm.core.TreeNode;
import com.renda.algorithm.core.TreeUtils;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * LeetCode 104 - Maximum Depth of Binary Tree.
 */
public class LC104_MaximumDepthOfBinaryTree implements AlgorithmProblem {

    @Override
    public ProblemSource getSource() {
        return ProblemSource.LEETCODE;
    }

    public int maxDepth(TreeNode root) {
        if (root == null) return 0;
        int count = 0;
        Queue<TreeNode> queue = new ArrayDeque<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            int size = queue.size();
            count++;
            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();
                if (node == null) break;
                if (node.left != null) queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
        }
        return count;
    }

    @Override
    public String getProblemId() {
        return "104";
    }

    @Override
    public void execute() {
        TreeNode root = TreeUtils.buildTree(new Integer[]{3,9,20,null,null,15,7});
        System.out.println("LC104 Maximum Depth of Binary Tree: " + maxDepth(root));
    }
}
