package compiler;

import java.util.*;

/**
 * Phase 6 — Target Code Generator.
 * Translates optimized TAC into x86-like assembly with register allocation.
 * Supports C-like constructs: printf calls, return, inc/dec, modulo.
 */
public class TargetCodeGen {

    private final List<String> assembly = new ArrayList<>();
    private final String[] REGS = { "R0", "R1", "R2", "R3", "R4", "R5", "R6", "R7" };
    private final Map<String, String> regMap = new LinkedHashMap<>();
    private int nextReg = 0;

    public List<String> getAssembly() {
        return assembly;
    }

    /** Allocate or return existing register for a variable/temp */
    private String getReg(String var) {
        if (regMap.containsKey(var))
            return regMap.get(var);
        String reg = (nextReg < REGS.length) ? REGS[nextReg++] : "MEM[" + var + "]";
        regMap.put(var, reg);
        return reg;
    }

    /** Check if a string is a numeric literal */
    private boolean isLiteral(String s) {
        return s != null && s.matches("-?\\d+(\\.\\d+)?");
    }

    /** Check if a string is a string/char literal */
    private boolean isStringLiteral(String s) {
        return s != null && (s.startsWith("\"") || s.startsWith("'"));
    }

    /* -- entry -------------------------------------------------- */
    public void generate(List<IntermediateCodeGen.TACInstruction> code) {
        assembly.clear();
        regMap.clear();
        nextReg = 0;

        for (IntermediateCodeGen.TACInstruction i : code) {
            switch (i.type) {
                case "copy":
                    genCopy(i);
                    break;
                case "binary":
                    genBinary(i);
                    break;
                case "unary":
                    genUnary(i);
                    break;
                case "label":
                    genLabel(i);
                    break;
                case "goto":
                    genGoto(i);
                    break;
                case "if_false_goto":
                    genIfFalseGoto(i);
                    break;
                case "print":
                    genPrint(i);
                    break;
                case "printf":
                    genPrintfDirect(i);
                    break;
                case "param":
                    genParam(i);
                    break;
                case "call":
                    genCall(i);
                    break;
                case "return":
                    genReturn(i);
                    break;
                case "inc":
                    genInc(i);
                    break;
                case "dec":
                    genDec(i);
                    break;
            }
        }
        assembly.add("");
        assembly.add("HALT");
        assembly.add("");
        assembly.add("; ---- Register Allocation Map ----");
        for (Map.Entry<String, String> e : regMap.entrySet()) {
            assembly.add(";   " + e.getKey() + " -> " + e.getValue());
        }
    }

    /* -- instruction generators --------------------------------- */
    private void genCopy(IntermediateCodeGen.TACInstruction i) {
        String dest = getReg(i.result);
        if (isLiteral(i.arg1)) {
            assembly.add("    MOV  " + dest + ", #" + i.arg1);
        } else if (isStringLiteral(i.arg1)) {
            assembly.add("    LEA  " + dest + ", " + i.arg1);
        } else {
            String src = getReg(i.arg1);
            assembly.add("    MOV  " + dest + ", " + src);
        }
    }

    private void genBinary(IntermediateCodeGen.TACInstruction i) {
        String dest = getReg(i.result);
        String left, right;
        // Load left operand
        if (isLiteral(i.arg1)) {
            assembly.add("    MOV  " + dest + ", #" + i.arg1);
            left = dest;
        } else {
            left = getReg(i.arg1);
            assembly.add("    MOV  " + dest + ", " + left);
        }
        // Resolve right operand
        if (isLiteral(i.arg2)) {
            right = "#" + i.arg2;
        } else {
            right = getReg(i.arg2);
        }
        // Emit operation
        switch (i.op) {
            case "+":
                assembly.add("    ADD  " + dest + ", " + right);
                break;
            case "-":
                assembly.add("    SUB  " + dest + ", " + right);
                break;
            case "*":
                assembly.add("    MUL  " + dest + ", " + right);
                break;
            case "/":
                assembly.add("    DIV  " + dest + ", " + right);
                break;
            case "%":
                assembly.add("    MOD  " + dest + ", " + right);
                break;
            case "==":
                assembly.add("    CMP  " + dest + ", " + right);
                assembly.add("    SETE " + dest);
                break;
            case "!=":
                assembly.add("    CMP  " + dest + ", " + right);
                assembly.add("    SETNE " + dest);
                break;
            case "<":
                assembly.add("    CMP  " + dest + ", " + right);
                assembly.add("    SETL " + dest);
                break;
            case ">":
                assembly.add("    CMP  " + dest + ", " + right);
                assembly.add("    SETG " + dest);
                break;
            case "<=":
                assembly.add("    CMP  " + dest + ", " + right);
                assembly.add("    SETLE " + dest);
                break;
            case ">=":
                assembly.add("    CMP  " + dest + ", " + right);
                assembly.add("    SETGE " + dest);
                break;
            default:
                assembly.add("    ; unknown op: " + i.op);
                break;
        }
    }

    private void genUnary(IntermediateCodeGen.TACInstruction i) {
        String dest = getReg(i.result);
        String src = isLiteral(i.arg1) ? "#" + i.arg1 : getReg(i.arg1);
        if ("-".equals(i.op)) {
            assembly.add("    MOV  " + dest + ", " + src);
            assembly.add("    NEG  " + dest);
        } else if ("!".equals(i.op)) {
            assembly.add("    MOV  " + dest + ", " + src);
            assembly.add("    NOT  " + dest);
        }
    }

    private void genLabel(IntermediateCodeGen.TACInstruction i) {
        assembly.add(i.label + ":");
    }

    private void genGoto(IntermediateCodeGen.TACInstruction i) {
        assembly.add("    JMP  " + i.jumpTarget);
    }

    private void genIfFalseGoto(IntermediateCodeGen.TACInstruction i) {
        String src = isLiteral(i.arg1) ? "#" + i.arg1 : getReg(i.arg1);
        assembly.add("    CMP  " + src + ", #0");
        assembly.add("    JE   " + i.jumpTarget);
    }

    private void genPrint(IntermediateCodeGen.TACInstruction i) {
        String src = isLiteral(i.arg1) ? "#" + i.arg1 : getReg(i.arg1);
        assembly.add("    PUSH " + src);
        assembly.add("    CALL _print");
    }

    private void genPrintfDirect(IntermediateCodeGen.TACInstruction i) {
        assembly.add("    PUSH " + i.arg1);
        assembly.add("    CALL _printf");
    }

    private void genParam(IntermediateCodeGen.TACInstruction i) {
        if (isLiteral(i.arg1)) {
            assembly.add("    PUSH #" + i.arg1);
        } else if (isStringLiteral(i.arg1)) {
            assembly.add("    PUSH " + i.arg1);
        } else {
            String src = getReg(i.arg1);
            assembly.add("    PUSH " + src);
        }
    }

    private void genCall(IntermediateCodeGen.TACInstruction i) {
        assembly.add("    CALL _" + i.arg1);
    }

    private void genReturn(IntermediateCodeGen.TACInstruction i) {
        if (i.arg1 != null) {
            if (isLiteral(i.arg1)) {
                assembly.add("    MOV  R0, #" + i.arg1);
            } else {
                String src = getReg(i.arg1);
                assembly.add("    MOV  R0, " + src);
            }
        }
        assembly.add("    RET");
    }

    private void genInc(IntermediateCodeGen.TACInstruction i) {
        String reg = getReg(i.result);
        assembly.add("    INC  " + reg);
    }

    private void genDec(IntermediateCodeGen.TACInstruction i) {
        String reg = getReg(i.result);
        assembly.add("    DEC  " + reg);
    }

    /** Formatted listing */
    public String toListing() {
        StringBuilder sb = new StringBuilder();
        for (String line : assembly)
            sb.append(line).append("\n");
        return sb.toString();
    }
}
