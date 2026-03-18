package com.renda.algorithm;

import com.renda.algorithm.core.*;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * 算法实验室统一启动入口 (Strategy Pattern + Abstract Factory)
 */
public class Main {
    public static void main(String[] args) {
        ProblemFactory factory = new ProblemRegistry();
        ProblemStrategy strategy = new DefaultProblemStrategy();
        List<AlgorithmProblem> problems = factory.getProblems();

        if (args != null && args.length > 0) {
            String token = String.join(" ", args).trim();
            if (!token.isBlank()) {
                if (isRunAllToken(token)) {
                    strategy.runAll(problems);
                    return;
                }
                Optional<AlgorithmProblem> selected = findProblem(problems, token);
                if (selected.isPresent()) {
                    strategy.runProblem(selected.get());
                    return;
                }
            }
        }

        runInteractiveMenu(strategy, problems);
    }

    private static void runInteractiveMenu(ProblemStrategy strategy, List<AlgorithmProblem> problems) {
        printMenu(problems);
        System.out.println("Algorithm Laboratory");
        System.out.println("Type a number to run, 'all' to run all, or 'q' to quit.");
        System.out.println("Support direct execution via startup parameters (avoiding interaction): ");
        System.out.println("all / 1 / LC1 / LEETCODE:1 etc. (will match ProblemSource:ProblemId or ProblemId)");
        System.out.println("Example: LC1, LEETCODE:1, 1, a, 0, --all");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) {
                    return;
                }
                String input = scanner.nextLine().trim();
                if (input.isBlank()) {
                    continue;
                }
                if (isQuitToken(input)) {
                    return;
                }
                if (isRunAllToken(input)) {
                    strategy.runAll(problems);
                    continue;
                }
                Integer index = tryParseInt(input);
                if (index != null) {
                    int i = index - 1;
                    if (i >= 0 && i < problems.size()) {
                        strategy.runProblem(problems.get(i));
                    } else {
                        System.out.println("Invalid selection: " + input);
                    }
                    continue;
                }

                Optional<AlgorithmProblem> selected = findProblem(problems, input);
                if (selected.isPresent()) {
                    strategy.runProblem(selected.get());
                    continue;
                }

                System.out.println("Unrecognized input: " + input);
            }
        }
    }

    private static void printMenu(List<AlgorithmProblem> problems) {
        for (int i = 0; i < problems.size(); i++) {
            AlgorithmProblem p = problems.get(i);
            String key = p.getSource() + ":" + p.getProblemId();
            System.out.println((i + 1) + ") " + key + " (" + p.getClass().getSimpleName() + ")");
        }
    }

    private static boolean isRunAllToken(String token) {
        String t = token.trim().toLowerCase();
        return t.equals("all") || t.equals("a") || t.equals("0") || t.equals("--all");
    }

    private static boolean isQuitToken(String token) {
        String t = token.trim().toLowerCase();
        return t.equals("q") || t.equals("quit") || t.equals("exit");
    }

    private static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Optional<AlgorithmProblem> findProblem(List<AlgorithmProblem> problems, String token) {
        String raw = token.trim();
        if (raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.replace(" ", "").toUpperCase();
        String normalizedId = normalized;
        if (normalized.startsWith("LC")) {
            String digits = normalized.substring(2).replaceAll("\\D+", "");
            if (!digits.isBlank()) {
                normalizedId = digits;
            }
        }
        for (AlgorithmProblem p : problems) {
            String key = (p.getSource() + ":" + p.getProblemId()).replace(" ", "").toUpperCase();
            String id = String.valueOf(p.getProblemId()).replace(" ", "").toUpperCase();
            if (key.equals(normalized) || id.equals(normalized) || id.equals(normalizedId)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
