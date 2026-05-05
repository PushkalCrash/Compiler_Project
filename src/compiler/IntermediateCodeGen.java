package compiler;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4 — Intermediate Code Generator.
 * Traverses the AST and emits Three-Address Code (TAC).
 * Supports C-like constructs: for, return, printf, compound assignments, ++/--.
 */
public class IntermediateCodeGen {

    /** A single TAC instruction */
    public static class TACInstruction {
        public String result, arg1, op, arg2;
        public String label;          // if this line is a label
        public String jumpTarget;     // for goto / conditional jumps
        public String type;           // "assign","binary","unary","copy","goto","if_goto","if_false_goto","label","print","printf","param","call","return","inc","dec"

        @Override
        public String toString() {
            switch (type) {
                case "label":         return label + ":";
                case "binary":        return result + " = " + arg1 + " " + op + " " + arg2;
                case "unary":         return result + " = " + op + " " + arg1;
                case "copy":          return result + " = " + arg1;
                case "goto":          return "goto " + jumpTarget;
                case "if_false_goto": return "if_false " + arg1 + " goto " + jumpTarget;
                case "print":         return "print " + arg1;
                case "printf":        return "printf " + arg1;
                case "param":         return "param " + arg1;
                case "call":          return "call " + arg1;
                case "return":        return "return" + (arg1 != null ? " " + arg1 : "");
                case "inc":           return result + " = " + result + " + 1";
                case "dec":           return result + " = " + result + " - 1";
                default:              return result + " = " + arg1 + " " + op + " " + arg2;
            }
        }
    }

    private final List<TACInstruction> code = new ArrayList<>();
    private int tempCount = 0;
    private int labelCount = 0;

    public List<TACInstruction> getCode() { return code; }

    private String newTemp()  { return "t" + (tempCount++); }
    private String newLabel() { return "L" + (labelCount++); }

    /* -- entry -------------------------------------------------- */
    public void generate(ASTNode node) {
        for (ASTNode child : node.getChildren()) genTopLevel(child);
    }

    private void genTopLevel(ASTNode n) {
        switch (n.getNodeType()) {
            case PREPROCESSOR:
                // Skip
                break;
            case FUNCTION_DEF:
                emitLabel("func_" + n.getName());
                if (!n.getChildren().isEmpty()) genBlock(n.getChildren().get(0));
                break;
            default:
                genStmt(n);
                break;
        }
    }

    /* -- statements --------------------------------------------- */
    private void genStmt(ASTNode n) {
        switch (n.getNodeType()) {
            case VAR_DECL: {
                String val = genExpr(n.getChildren().get(0));
                emit("copy", n.getName(), val, null, null);
                break;
            }
            case ASSIGN: {
                String val = genExpr(n.getChildren().get(0));
                emit("copy", n.getName(), val, null, null);
                break;
            }
            case COMPOUND_ASSIGN: {
                String val = genExpr(n.getChildren().get(0));
                String opChar = n.getValue().substring(0, 1); // += -> +, -= -> -, etc.
                String t = newTemp();
                emit("binary", t, n.getName(), opChar, val);
                emit("copy", n.getName(), t, null, null);
                break;
            }
            case POST_INCREMENT: {
                TACInstruction i = new TACInstruction();
                i.type = "inc"; i.result = n.getValue();
                code.add(i);
                break;
            }
            case POST_DECREMENT: {
                TACInstruction i = new TACInstruction();
                i.type = "dec"; i.result = n.getValue();
                code.add(i);
                break;
            }
            case PRINT: {
                String val = genExpr(n.getChildren().get(0));
                TACInstruction i = new TACInstruction();
                i.type = "print"; i.arg1 = val;
                code.add(i);
                break;
            }
            case PRINTF: {
                // Push all arguments as params, then call printf
                for (int idx = 0; idx < n.getChildren().size(); idx++) {
                    String val = genExpr(n.getChildren().get(idx));
                    TACInstruction pi = new TACInstruction();
                    pi.type = "param"; pi.arg1 = val;
                    code.add(pi);
                }
                TACInstruction ci = new TACInstruction();
                ci.type = "call"; ci.arg1 = "printf";
                code.add(ci);
                break;
            }
            case RETURN: {
                TACInstruction i = new TACInstruction();
                i.type = "return";
                if (!n.getChildren().isEmpty()) {
                    i.arg1 = genExpr(n.getChildren().get(0));
                }
                code.add(i);
                break;
            }
            case IF: {
                String cond = genExpr(n.getChildren().get(0));
                String elseLabel = newLabel();
                String endLabel  = newLabel();
                emitIfFalseGoto(cond, elseLabel);
                genBlock(n.getChildren().get(1));
                if (n.getChildren().size() > 2) {
                    emitGoto(endLabel);
                    emitLabel(elseLabel);
                    genBlock(n.getChildren().get(2));
                    emitLabel(endLabel);
                } else {
                    emitLabel(elseLabel);
                }
                break;
            }
            case WHILE: {
                String startLabel = newLabel();
                String endLabel   = newLabel();
                emitLabel(startLabel);
                String cond = genExpr(n.getChildren().get(0));
                emitIfFalseGoto(cond, endLabel);
                genBlock(n.getChildren().get(1));
                emitGoto(startLabel);
                emitLabel(endLabel);
                break;
            }
            case FOR: {
                // for(init; cond; update) body
                // init
                genStmt(n.getChildren().get(0));
                String startLabel = newLabel();
                String endLabel   = newLabel();
                emitLabel(startLabel);
                // condition
                String cond = genExpr(n.getChildren().get(1));
                emitIfFalseGoto(cond, endLabel);
                // body
                genBlock(n.getChildren().get(3));
                // update
                genStmt(n.getChildren().get(2));
                emitGoto(startLabel);
                emitLabel(endLabel);
                break;
            }
            case BLOCK: genBlock(n); break;
            case EMPTY_STMT: break;
            default: break;
        }
    }

    private void genBlock(ASTNode block) {
        for (ASTNode child : block.getChildren()) genStmt(child);
    }

    /* -- expressions -------------------------------------------- */
    private String genExpr(ASTNode n) {
        switch (n.getNodeType()) {
            case INTEGER_LITERAL:
            case FLOAT_LITERAL:   return n.getValue();
            case STRING_LITERAL:  return n.getValue();
            case CHAR_LITERAL:    return n.getValue();
            case IDENTIFIER:      return n.getValue();
            case POST_INCREMENT: {
                String t = newTemp();
                emit("copy", t, n.getValue(), null, null);
                TACInstruction inc = new TACInstruction();
                inc.type = "inc"; inc.result = n.getValue();
                code.add(inc);
                return t; // return old value
            }
            case POST_DECREMENT: {
                String t = newTemp();
                emit("copy", t, n.getValue(), null, null);
                TACInstruction dec = new TACInstruction();
                dec.type = "dec"; dec.result = n.getValue();
                code.add(dec);
                return t;
            }
            case BINARY_OP: {
                String l = genExpr(n.getChildren().get(0));
                String r = genExpr(n.getChildren().get(1));
                String t = newTemp();
                emit("binary", t, l, n.getValue(), r);
                return t;
            }
            case UNARY_OP: {
                String operand = genExpr(n.getChildren().get(0));
                String t = newTemp();
                emit("unary", t, operand, n.getValue(), null);
                return t;
            }
            default: return n.getValue() != null ? n.getValue() : "?";
        }
    }

    /* -- emit helpers ------------------------------------------- */
    private void emit(String type, String result, String arg1, String op, String arg2) {
        TACInstruction i = new TACInstruction();
        i.type = type; i.result = result; i.arg1 = arg1; i.op = op; i.arg2 = arg2;
        code.add(i);
    }

    private void emitLabel(String label) {
        TACInstruction i = new TACInstruction();
        i.type = "label"; i.label = label;
        code.add(i);
    }

    private void emitGoto(String target) {
        TACInstruction i = new TACInstruction();
        i.type = "goto"; i.jumpTarget = target;
        code.add(i);
    }

    private void emitIfFalseGoto(String cond, String target) {
        TACInstruction i = new TACInstruction();
        i.type = "if_false_goto"; i.arg1 = cond; i.jumpTarget = target;
        code.add(i);
    }

    /** Produce a formatted numbered listing of all TAC instructions */
    public String toListing() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < code.size(); i++) {
            sb.append(String.format("%3d:  %s%n", i, code.get(i)));
        }
        return sb.toString();
    }
}
