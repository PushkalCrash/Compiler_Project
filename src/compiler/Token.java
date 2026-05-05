package compiler;

/**
 * Represents a lexical token produced by the Lexer.
 * Each token has a type, value, line number, and category.
 */
public class Token {
    public enum Type {
        // Keywords
        INT, FLOAT, CHAR, IF, ELSE, WHILE, FOR, PRINT, PRINTF, RETURN,
        // Literals
        INTEGER_LITERAL, FLOAT_LITERAL, STRING_LITERAL, CHAR_LITERAL,
        // Identifiers
        IDENTIFIER,
        // Arithmetic Operators
        PLUS, MINUS, MULTIPLY, DIVIDE, MODULO,
        // Increment / Decrement
        INCREMENT, DECREMENT,
        // Assignment & Compound Assignment
        ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN, DIVIDE_ASSIGN,
        // Comparison
        EQUALS, NOT_EQUALS,
        LESS_THAN, GREATER_THAN, LESS_EQUAL, GREATER_EQUAL,
        // Logical
        AND, OR, NOT,
        // Delimiters
        LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET,
        SEMICOLON, COMMA,
        // Preprocessor
        PREPROCESSOR,
        // Special
        EOF, ERROR
    }

    private final Type type;
    private final String value;
    private final int line;

    public Token(Type type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }

    public Type getType() { return type; }
    public String getValue() { return value; }
    public int getLine() { return line; }

    /**
     * Returns the human-readable category for this token.
     * Categories: Keyword, Identifier, Operator, Bracket, Special Character, Literal
     */
    public String getCategory() {
        switch (type) {
            // Keywords
            case INT: case FLOAT: case CHAR: case IF: case ELSE:
            case WHILE: case FOR: case PRINT: case PRINTF: case RETURN:
                return "Keyword";
            // Identifiers
            case IDENTIFIER:
                return "Identifier";
            // Operators: + - * / % = == != < > <= >= && || ! ++ -- += -= *= /=
            case PLUS: case MINUS: case MULTIPLY: case DIVIDE: case MODULO:
            case INCREMENT: case DECREMENT:
            case ASSIGN: case PLUS_ASSIGN: case MINUS_ASSIGN:
            case MULTIPLY_ASSIGN: case DIVIDE_ASSIGN:
            case EQUALS: case NOT_EQUALS:
            case LESS_THAN: case GREATER_THAN: case LESS_EQUAL: case GREATER_EQUAL:
            case AND: case OR: case NOT:
                return "Operator";
            // Brackets: ( ) { } [ ]
            case LPAREN: case RPAREN: case LBRACE: case RBRACE:
            case LBRACKET: case RBRACKET:
                return "Bracket";
            // Special Characters: , ; "" '' #include
            case SEMICOLON: case COMMA:
            case STRING_LITERAL: case CHAR_LITERAL:
            case PREPROCESSOR:
                return "Special Character";
            // Literals
            case INTEGER_LITERAL: case FLOAT_LITERAL:
                return "Literal";
            // End of file
            case EOF:
                return "EOF";
            // Error
            case ERROR:
                return "Error";
            default:
                return "Unknown";
        }
    }

    @Override
    public String toString() {
        return String.format("<%s, %s, line:%d>", type, value, line);
    }
}
