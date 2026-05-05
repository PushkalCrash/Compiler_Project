package compiler;

import java.util.*;

/**
 * Phase 3 — Semantic Analyzer.
 * Performs type checking, scope analysis, and builds a symbol table.
 * Supports C-like constructs: for, return, printf, char, compound assignments.
 */
public class SemanticAnalyzer {

    /** Symbol-table entry */
    public static class Symbol {
        public final String name, type, scope;
        public final int line;
        public Symbol(String name, String type, String scope, int line) {
            this.name = name; this.type = type; this.scope = scope; this.line = line;
        }
    }

    private final List<Symbol> symbolTable = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();
    private int scopeDepth = 0;
    private String currentScope = "global";

    public List<Symbol> getSymbolTable() { return symbolTable; }
    public List<String> getErrors()      { return errors; }
    public List<String> getWarnings()    { return warnings; }

    /* -- scope helpers ----------------------------------------- */
    private void enterScope(String label) {
        scopes.push(new HashMap<>());
        scopeDepth++;
        currentScope = label + "_" + scopeDepth;
    }
    private void exitScope() { scopes.pop(); scopeDepth--; currentScope = scopeDepth == 0 ? "global" : "block_" + scopeDepth; }
    private void declare(String name, String type, int line) {
        Map<String, Symbol> top = scopes.peek();
        if (top.containsKey(name)) { errors.add("Line " + line + ": Variable '" + name + "' already declared in this scope"); return; }
        Symbol s = new Symbol(name, type, currentScope, line);
        top.put(name, s);
        symbolTable.add(s);
    }
    private Symbol lookup(String name) {
        for (Map<String, Symbol> scope : scopes) { if (scope.containsKey(name)) return scope.get(name); }
        return null;
    }

    /* -- entry -------------------------------------------------- */
    public void analyze(ASTNode node) {
        enterScope("global");
        for (ASTNode child : node.getChildren()) analyzeTopLevel(child);
        exitScope();
    }

    private void analyzeTopLevel(ASTNode n) {
        switch (n.getNodeType()) {
            case PREPROCESSOR:
                // Skip preprocessor directives
                break;
            case FUNCTION_DEF:
                enterScope("func_" + n.getName());
                if (!n.getChildren().isEmpty()) analyzeBlock(n.getChildren().get(0));
                exitScope();
                break;
            default:
                analyzeStmt(n);
                break;
        }
    }

    /* -- statements --------------------------------------------- */
    private void analyzeStmt(ASTNode n) {
        switch (n.getNodeType()) {
            case VAR_DECL: {
                String exprType = analyzeExpr(n.getChildren().get(0));
                declare(n.getName(), n.getDataType(), 0);
                if (!compatible(n.getDataType(), exprType))
                    warnings.add("Type mismatch: assigning " + exprType + " to " + n.getDataType() + " variable '" + n.getName() + "'");
                n.setInferredType(n.getDataType());
                break;
            }
            case ASSIGN: {
                Symbol sym = lookup(n.getName());
                if (sym == null) { errors.add("Variable '" + n.getName() + "' used before declaration"); break; }
                String rtype = analyzeExpr(n.getChildren().get(0));
                if (!compatible(sym.type, rtype))
                    warnings.add("Type mismatch: assigning " + rtype + " to " + sym.type + " variable '" + n.getName() + "'");
                n.setInferredType(sym.type);
                break;
            }
            case COMPOUND_ASSIGN: {
                Symbol sym = lookup(n.getName());
                if (sym == null) { errors.add("Variable '" + n.getName() + "' used before declaration"); break; }
                String rtype = analyzeExpr(n.getChildren().get(0));
                n.setInferredType(sym.type);
                break;
            }
            case POST_INCREMENT:
            case POST_DECREMENT: {
                Symbol sym = lookup(n.getValue());
                if (sym == null) { errors.add("Variable '" + n.getValue() + "' used before declaration"); }
                n.setInferredType(sym != null ? sym.type : "int");
                break;
            }
            case IF: {
                analyzeExpr(n.getChildren().get(0));
                enterScope("if_then"); analyzeBlock(n.getChildren().get(1)); exitScope();
                if (n.getChildren().size() > 2) { enterScope("if_else"); analyzeBlock(n.getChildren().get(2)); exitScope(); }
                break;
            }
            case WHILE: {
                analyzeExpr(n.getChildren().get(0));
                enterScope("while_body"); analyzeBlock(n.getChildren().get(1)); exitScope();
                break;
            }
            case FOR: {
                enterScope("for");
                // init
                analyzeStmt(n.getChildren().get(0));
                // condition
                analyzeExpr(n.getChildren().get(1));
                // update
                analyzeStmt(n.getChildren().get(2));
                // body
                analyzeBlock(n.getChildren().get(3));
                exitScope();
                break;
            }
            case PRINT: {
                String pt = analyzeExpr(n.getChildren().get(0));
                n.setInferredType(pt);
                break;
            }
            case PRINTF: {
                // Analyze all arguments
                for (ASTNode child : n.getChildren()) analyzeExpr(child);
                n.setInferredType("void");
                break;
            }
            case RETURN: {
                if (!n.getChildren().isEmpty()) {
                    String rt = analyzeExpr(n.getChildren().get(0));
                    n.setInferredType(rt);
                } else {
                    n.setInferredType("void");
                }
                break;
            }
            case BLOCK: {
                analyzeBlock(n);
                break;
            }
            case EMPTY_STMT:
                break;
            default: break;
        }
    }

    private void analyzeBlock(ASTNode block) {
        for (ASTNode child : block.getChildren()) analyzeStmt(child);
    }

    /* -- expressions -------------------------------------------- */
    private String analyzeExpr(ASTNode n) {
        switch (n.getNodeType()) {
            case INTEGER_LITERAL: n.setInferredType("int");   return "int";
            case FLOAT_LITERAL:   n.setInferredType("float"); return "float";
            case STRING_LITERAL:  n.setInferredType("string"); return "string";
            case CHAR_LITERAL:    n.setInferredType("char");  return "char";
            case IDENTIFIER: {
                Symbol s = lookup(n.getValue());
                if (s == null) { errors.add("Variable '" + n.getValue() + "' used before declaration"); n.setInferredType("int"); return "int"; }
                n.setInferredType(s.type); return s.type;
            }
            case POST_INCREMENT:
            case POST_DECREMENT: {
                Symbol s = lookup(n.getValue());
                if (s == null) { errors.add("Variable '" + n.getValue() + "' used before declaration"); n.setInferredType("int"); return "int"; }
                n.setInferredType(s.type); return s.type;
            }
            case BINARY_OP: {
                String lt = analyzeExpr(n.getChildren().get(0));
                String rt = analyzeExpr(n.getChildren().get(1));
                String op = n.getValue();
                if ("== != < > <= >=".contains(op)) { n.setInferredType("int"); return "int"; }
                if ("&& ||".contains(op))           { n.setInferredType("int"); return "int"; }
                String res = (lt.equals("float") || rt.equals("float")) ? "float" : "int";
                n.setInferredType(res); return res;
            }
            case UNARY_OP: {
                String ut = analyzeExpr(n.getChildren().get(0));
                n.setInferredType(ut); return ut;
            }
            default: n.setInferredType("int"); return "int";
        }
    }

    private boolean compatible(String target, String source) {
        if (target.equals(source)) return true;
        if (target.equals("float") && source.equals("int")) return true; // implicit widening
        if (target.equals("char") && source.equals("int")) return true;  // char/int compatible in C
        if (target.equals("int") && source.equals("char")) return true;
        return false;
    }
}
