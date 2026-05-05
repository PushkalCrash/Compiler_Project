package compiler;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the Abstract Syntax Tree.
 * Supports various node types for a C-like language.
 */
public class ASTNode {
    public enum NodeType {
        PROGRAM, VAR_DECL, ASSIGN, IF, WHILE, FOR, PRINT, PRINTF, RETURN, BLOCK,
        BINARY_OP, UNARY_OP, POST_INCREMENT, POST_DECREMENT,
        COMPOUND_ASSIGN,
        IDENTIFIER, INTEGER_LITERAL, FLOAT_LITERAL, STRING_LITERAL, CHAR_LITERAL,
        PREPROCESSOR, FUNCTION_DEF, EMPTY_STMT
    }

    private final NodeType type;
    private String value;
    private String dataType;
    private String name;
    private final List<ASTNode> children = new ArrayList<>();
    private String inferredType;

    public ASTNode(NodeType type) {
        this.type = type;
    }

    public ASTNode(NodeType type, String value) {
        this.type = type;
        this.value = value;
    }

    // Factory methods
    public static ASTNode program() { return new ASTNode(NodeType.PROGRAM); }

    public static ASTNode varDecl(String dataType, String name) {
        ASTNode n = new ASTNode(NodeType.VAR_DECL);
        n.dataType = dataType;
        n.name = name;
        return n;
    }

    public static ASTNode assign(String name) {
        ASTNode n = new ASTNode(NodeType.ASSIGN);
        n.name = name;
        return n;
    }

    public static ASTNode compoundAssign(String name, String op) {
        ASTNode n = new ASTNode(NodeType.COMPOUND_ASSIGN, op);
        n.name = name;
        return n;
    }

    public static ASTNode ifNode()      { return new ASTNode(NodeType.IF); }
    public static ASTNode whileNode()   { return new ASTNode(NodeType.WHILE); }
    public static ASTNode forNode()     { return new ASTNode(NodeType.FOR); }
    public static ASTNode printNode()   { return new ASTNode(NodeType.PRINT); }
    public static ASTNode printfNode()  { return new ASTNode(NodeType.PRINTF); }
    public static ASTNode returnNode()  { return new ASTNode(NodeType.RETURN); }
    public static ASTNode block()       { return new ASTNode(NodeType.BLOCK); }
    public static ASTNode emptyStmt()   { return new ASTNode(NodeType.EMPTY_STMT); }
    public static ASTNode binaryOp(String op)   { return new ASTNode(NodeType.BINARY_OP, op); }
    public static ASTNode unaryOp(String op)    { return new ASTNode(NodeType.UNARY_OP, op); }
    public static ASTNode postIncrement(String name) { return new ASTNode(NodeType.POST_INCREMENT, name); }
    public static ASTNode postDecrement(String name) { return new ASTNode(NodeType.POST_DECREMENT, name); }
    public static ASTNode identifier(String name)    { return new ASTNode(NodeType.IDENTIFIER, name); }
    public static ASTNode intLiteral(String val)     { return new ASTNode(NodeType.INTEGER_LITERAL, val); }
    public static ASTNode floatLiteral(String val)   { return new ASTNode(NodeType.FLOAT_LITERAL, val); }
    public static ASTNode stringLiteral(String val)  { return new ASTNode(NodeType.STRING_LITERAL, val); }
    public static ASTNode charLiteral(String val)    { return new ASTNode(NodeType.CHAR_LITERAL, val); }
    public static ASTNode preprocessor(String val)   { return new ASTNode(NodeType.PREPROCESSOR, val); }

    public static ASTNode functionDef(String returnType, String name) {
        ASTNode n = new ASTNode(NodeType.FUNCTION_DEF);
        n.dataType = returnType;
        n.name = name;
        return n;
    }

    public void addChild(ASTNode child) { children.add(child); }

    // Getters
    public NodeType getNodeType() { return type; }
    public String getValue() { return value; }
    public String getDataType() { return dataType; }
    public String getName() { return name; }
    public List<ASTNode> getChildren() { return children; }
    public String getInferredType() { return inferredType; }
    public void setInferredType(String t) { this.inferredType = t; }

    /** Returns a human-readable label for tree display */
    public String getLabel() {
        switch (type) {
            case PROGRAM:          return "Program";
            case VAR_DECL:         return "VarDecl(" + dataType + " " + name + ")";
            case ASSIGN:           return "Assign(" + name + ")";
            case COMPOUND_ASSIGN:  return "CompoundAssign(" + name + " " + value + ")";
            case IF:               return "If";
            case WHILE:            return "While";
            case FOR:              return "For";
            case PRINT:            return "Print";
            case PRINTF:           return "Printf";
            case RETURN:           return "Return";
            case BLOCK:            return "Block";
            case BINARY_OP:        return "Op(" + value + ")";
            case UNARY_OP:         return "Unary(" + value + ")";
            case POST_INCREMENT:   return "PostInc(" + value + ")";
            case POST_DECREMENT:   return "PostDec(" + value + ")";
            case IDENTIFIER:       return "Id(" + value + ")";
            case INTEGER_LITERAL:  return "Int(" + value + ")";
            case FLOAT_LITERAL:    return "Float(" + value + ")";
            case STRING_LITERAL:   return "Str(" + value + ")";
            case CHAR_LITERAL:     return "Char(" + value + ")";
            case PREPROCESSOR:     return "Preprocessor(" + value + ")";
            case FUNCTION_DEF:     return "FuncDef(" + dataType + " " + name + ")";
            case EMPTY_STMT:       return "EmptyStmt";
            default: return type.toString();
        }
    }

    /** Produces an indented tree-string representation */
    public String toTreeString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(getLabel());
        if (inferredType != null) sb.append(" [").append(inferredType).append("]");
        sb.append("\n");
        for (ASTNode c : children) sb.append(c.toTreeString(indent + "  "));
        return sb.toString();
    }

    @Override
    public String toString() { return toTreeString(""); }
}
