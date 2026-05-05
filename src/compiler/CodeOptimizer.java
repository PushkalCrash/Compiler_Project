package compiler;

import java.util.*;

/**
 * Phase 5 — Code Optimizer.
 * Applies optimizations to Three-Address Code:
 *   1. Constant folding
 *   2. Constant propagation
 *   3. Dead code elimination
 *   4. Strength reduction
 */
public class CodeOptimizer {

    private List<IntermediateCodeGen.TACInstruction> original;
    private List<IntermediateCodeGen.TACInstruction> optimized;
    private final List<String> applied = new ArrayList<>();

    public List<IntermediateCodeGen.TACInstruction> getOptimized() { return optimized; }
    public List<String> getApplied() { return applied; }

    public void optimize(List<IntermediateCodeGen.TACInstruction> code) {
        // Deep-copy
        optimized = new ArrayList<>();
        for (IntermediateCodeGen.TACInstruction src : code) {
            IntermediateCodeGen.TACInstruction copy = new IntermediateCodeGen.TACInstruction();
            copy.type = src.type; copy.result = src.result; copy.arg1 = src.arg1;
            copy.op = src.op; copy.arg2 = src.arg2; copy.label = src.label; copy.jumpTarget = src.jumpTarget;
            optimized.add(copy);
        }
        constantFolding();
        constantPropagation();
        strengthReduction();
        deadCodeElimination();
    }

    /* -- 1. Constant Folding ------------------------------------- */
    private void constantFolding() {
        boolean changed = false;
        for (IntermediateCodeGen.TACInstruction i : optimized) {
            if (!"binary".equals(i.type)) continue;
            Integer a = tryInt(i.arg1), b = tryInt(i.arg2);
            if (a != null && b != null) {
                Integer result = evalInt(a, i.op, b);
                if (result != null) {
                    i.type = "copy"; i.arg1 = String.valueOf(result); i.op = null; i.arg2 = null;
                    changed = true;
                }
            }
        }
        if (changed) applied.add("Constant Folding");
    }

    /* -- 2. Constant Propagation --------------------------------- */
    private void constantPropagation() {
        boolean changed = false;
        Map<String, String> constants = new LinkedHashMap<>();
        for (IntermediateCodeGen.TACInstruction i : optimized) {
            // Track constant copies
            if ("copy".equals(i.type) && isConst(i.arg1)) constants.put(i.result, i.arg1);
            // Propagate into operands
            if ("binary".equals(i.type) || "copy".equals(i.type) || "unary".equals(i.type)
                || "if_false_goto".equals(i.type) || "print".equals(i.type) || "param".equals(i.type)) {
                if (i.arg1 != null && constants.containsKey(i.arg1)) { i.arg1 = constants.get(i.arg1); changed = true; }
                if (i.arg2 != null && constants.containsKey(i.arg2)) { i.arg2 = constants.get(i.arg2); changed = true; }
            }
            // Re-check for folding after propagation
            if ("binary".equals(i.type)) {
                Integer a = tryInt(i.arg1), b = tryInt(i.arg2);
                if (a != null && b != null) {
                    Integer result = evalInt(a, i.op, b);
                    if (result != null) {
                        i.type = "copy"; i.arg1 = String.valueOf(result); i.op = null; i.arg2 = null;
                        constants.put(i.result, i.arg1);
                    }
                }
            }
        }
        if (changed) applied.add("Constant Propagation");
    }

    /* -- 3. Strength Reduction ----------------------------------- */
    private void strengthReduction() {
        boolean changed = false;
        for (IntermediateCodeGen.TACInstruction i : optimized) {
            if (!"binary".equals(i.type)) continue;
            Integer b = tryInt(i.arg2);
            if ("*".equals(i.op) && b != null && b == 2) {
                i.op = "+"; i.arg2 = i.arg1; changed = true;
            }
            if ("*".equals(i.op) && b != null && b == 0) {
                i.type = "copy"; i.arg1 = "0"; i.op = null; i.arg2 = null; changed = true;
            }
            if ("*".equals(i.op) && b != null && b == 1) {
                i.type = "copy"; i.op = null; i.arg2 = null; changed = true;
            }
            if ("+".equals(i.op) && b != null && b == 0) {
                i.type = "copy"; i.op = null; i.arg2 = null; changed = true;
            }
        }
        if (changed) applied.add("Strength Reduction");
    }

    /* -- 4. Dead Code Elimination -------------------------------- */
    private void deadCodeElimination() {
        // Find all variables that are actually *used* as arg1/arg2 (not as result)
        Set<String> used = new HashSet<>();
        for (IntermediateCodeGen.TACInstruction i : optimized) {
            if (i.arg1 != null && !isConst(i.arg1)) used.add(i.arg1);
            if (i.arg2 != null && !isConst(i.arg2)) used.add(i.arg2);
        }
        // Preserve non-temp assignments and any instruction whose result is used
        int before = optimized.size();
        optimized.removeIf(i -> {
            if (!"copy".equals(i.type) && !"binary".equals(i.type) && !"unary".equals(i.type)) return false;
            if (i.result == null) return false;
            if (!i.result.startsWith("t")) return false;       // keep user vars
            return !used.contains(i.result);
        });
        if (optimized.size() < before) applied.add("Dead Code Elimination");
    }

    /* -- helpers ------------------------------------------------- */
    private static Integer tryInt(String s) {
        if (s == null) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }
    private static boolean isConst(String s) { return s != null && s.matches("-?\\d+(\\.\\d+)?"); }
    private static Integer evalInt(int a, String op, int b) {
        switch (op) {
            case "+": return a + b; case "-": return a - b;
            case "*": return a * b; case "/": return b != 0 ? a / b : null;
            case "%": return b != 0 ? a % b : null;
            case "==": return a == b ? 1 : 0; case "!=": return a != b ? 1 : 0;
            case "<": return a < b ? 1 : 0;  case ">": return a > b ? 1 : 0;
            case "<=": return a <= b ? 1 : 0; case ">=": return a >= b ? 1 : 0;
            default: return null;
        }
    }

    /** Numbered listing */
    public String toListing() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < optimized.size(); i++)
            sb.append(String.format("%3d:  %s%n", i, optimized.get(i)));
        if (!applied.isEmpty()) {
            sb.append("\n-- Optimizations Applied --\n");
            for (String a : applied) sb.append("  * ").append(a).append("\n");
        }
        return sb.toString();
    }
}
