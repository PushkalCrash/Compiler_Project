package compiler;

import java.util.*;

/**
 * Phase 1 — Lexical Analyzer.
 * Scans source code character-by-character and produces a list of Tokens.
 * Supports C-like keywords, identifiers, numbers, operators, delimiters,
 * string/char literals, preprocessor directives, and comments.
 */
public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private int pos = 0;
    private int line = 1;

    private static final Map<String, Token.Type> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("int",    Token.Type.INT);
        KEYWORDS.put("float",  Token.Type.FLOAT);
        KEYWORDS.put("char",   Token.Type.CHAR);
        KEYWORDS.put("if",     Token.Type.IF);
        KEYWORDS.put("else",   Token.Type.ELSE);
        KEYWORDS.put("while",  Token.Type.WHILE);
        KEYWORDS.put("for",    Token.Type.FOR);
        KEYWORDS.put("print",  Token.Type.PRINT);
        KEYWORDS.put("printf", Token.Type.PRINTF);
        KEYWORDS.put("return", Token.Type.RETURN);
    }

    public Lexer(String source) { this.source = source; }

    public List<Token> tokenize() {
        while (pos < source.length()) {
            skipWhitespaceAndComments();
            if (pos >= source.length()) break;
            char c = source.charAt(pos);

            // Preprocessor directives: #include, #define, etc.
            if (c == '#') { scanPreprocessor(); continue; }
            // String literals
            if (c == '"') { scanStringLiteral(); continue; }
            // Char literals
            if (c == '\'') { scanCharLiteral(); continue; }
            // Identifiers and keywords
            if (Character.isLetter(c) || c == '_') { scanIdentifier(); continue; }
            // Numbers
            if (Character.isDigit(c)) { scanNumber(); continue; }
            // Operators and delimiters
            scanOperator();
        }
        tokens.add(new Token(Token.Type.EOF, "EOF", line));
        return tokens;
    }

    private void skipWhitespaceAndComments() {
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\r') { pos++; }
            else if (c == '\n') { pos++; line++; }
            else if (c == '/' && pos + 1 < source.length() && source.charAt(pos + 1) == '/') {
                // Single-line comment
                while (pos < source.length() && source.charAt(pos) != '\n') pos++;
            } else if (c == '/' && pos + 1 < source.length() && source.charAt(pos + 1) == '*') {
                // Multi-line comment
                pos += 2;
                while (pos + 1 < source.length() && !(source.charAt(pos) == '*' && source.charAt(pos + 1) == '/')) {
                    if (source.charAt(pos) == '\n') line++;
                    pos++;
                }
                if (pos + 1 < source.length()) pos += 2;
            } else break;
        }
    }

    /** Scan a preprocessor directive: absorb the entire line as a single token */
    private void scanPreprocessor() {
        int start = pos;
        while (pos < source.length() && source.charAt(pos) != '\n') pos++;
        String directive = source.substring(start, pos).trim();
        tokens.add(new Token(Token.Type.PREPROCESSOR, directive, line));
    }

    /** Scan a string literal "..." with escape sequences (\n, \t, \\, \") */
    private void scanStringLiteral() {
        int startLine = line;
        pos++; // skip opening "
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        while (pos < source.length() && source.charAt(pos) != '"') {
            char c = source.charAt(pos);
            if (c == '\\' && pos + 1 < source.length()) {
                char next = source.charAt(pos + 1);
                sb.append('\\').append(next);
                pos += 2;
                continue;
            }
            if (c == '\n') line++;
            sb.append(c);
            pos++;
        }
        if (pos < source.length()) {
            sb.append('"');
            pos++; // skip closing "
        } else {
            errors.add("Line " + startLine + ": Unterminated string literal");
        }
        tokens.add(new Token(Token.Type.STRING_LITERAL, sb.toString(), startLine));
    }

    /** Scan a character literal 'x' with escape sequences */
    private void scanCharLiteral() {
        int startLine = line;
        pos++; // skip opening '
        StringBuilder sb = new StringBuilder();
        sb.append('\'');
        if (pos < source.length()) {
            char c = source.charAt(pos);
            if (c == '\\' && pos + 1 < source.length()) {
                sb.append('\\').append(source.charAt(pos + 1));
                pos += 2;
            } else {
                sb.append(c);
                pos++;
            }
        }
        if (pos < source.length() && source.charAt(pos) == '\'') {
            sb.append('\'');
            pos++; // skip closing '
        } else {
            errors.add("Line " + startLine + ": Unterminated character literal");
        }
        tokens.add(new Token(Token.Type.CHAR_LITERAL, sb.toString(), startLine));
    }

    private void scanIdentifier() {
        int start = pos;
        while (pos < source.length() && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_')) pos++;
        String word = source.substring(start, pos);
        tokens.add(new Token(KEYWORDS.getOrDefault(word, Token.Type.IDENTIFIER), word, line));
    }

    private void scanNumber() {
        int start = pos;
        boolean isFloat = false;
        while (pos < source.length() && Character.isDigit(source.charAt(pos))) pos++;
        if (pos < source.length() && source.charAt(pos) == '.' && pos + 1 < source.length() && Character.isDigit(source.charAt(pos + 1))) {
            isFloat = true; pos++;
            while (pos < source.length() && Character.isDigit(source.charAt(pos))) pos++;
        }
        tokens.add(new Token(isFloat ? Token.Type.FLOAT_LITERAL : Token.Type.INTEGER_LITERAL, source.substring(start, pos), line));
    }

    private void scanOperator() {
        char c = source.charAt(pos);
        char next = (pos + 1 < source.length()) ? source.charAt(pos + 1) : '\0';
        switch (c) {
            case '+':
                if (next == '+') { tokens.add(new Token(Token.Type.INCREMENT, "++", line)); pos += 2; }
                else if (next == '=') { tokens.add(new Token(Token.Type.PLUS_ASSIGN, "+=", line)); pos += 2; }
                else add(Token.Type.PLUS, "+");
                break;
            case '-':
                if (next == '-') { tokens.add(new Token(Token.Type.DECREMENT, "--", line)); pos += 2; }
                else if (next == '=') { tokens.add(new Token(Token.Type.MINUS_ASSIGN, "-=", line)); pos += 2; }
                else add(Token.Type.MINUS, "-");
                break;
            case '*':
                if (next == '=') { tokens.add(new Token(Token.Type.MULTIPLY_ASSIGN, "*=", line)); pos += 2; }
                else add(Token.Type.MULTIPLY, "*");
                break;
            case '/':
                if (next == '=') { tokens.add(new Token(Token.Type.DIVIDE_ASSIGN, "/=", line)); pos += 2; }
                else add(Token.Type.DIVIDE, "/");
                break;
            case '%': add(Token.Type.MODULO, "%"); break;
            case '(': add(Token.Type.LPAREN, "("); break;
            case ')': add(Token.Type.RPAREN, ")"); break;
            case '{': add(Token.Type.LBRACE, "{"); break;
            case '}': add(Token.Type.RBRACE, "}"); break;
            case '[': add(Token.Type.LBRACKET, "["); break;
            case ']': add(Token.Type.RBRACKET, "]"); break;
            case ';': add(Token.Type.SEMICOLON, ";"); break;
            case ',': add(Token.Type.COMMA, ","); break;
            case '=':
                if (next == '=') { tokens.add(new Token(Token.Type.EQUALS, "==", line)); pos += 2; }
                else add(Token.Type.ASSIGN, "=");
                break;
            case '!':
                if (next == '=') { tokens.add(new Token(Token.Type.NOT_EQUALS, "!=", line)); pos += 2; }
                else add(Token.Type.NOT, "!");
                break;
            case '<':
                if (next == '=') { tokens.add(new Token(Token.Type.LESS_EQUAL, "<=", line)); pos += 2; }
                else add(Token.Type.LESS_THAN, "<");
                break;
            case '>':
                if (next == '=') { tokens.add(new Token(Token.Type.GREATER_EQUAL, ">=", line)); pos += 2; }
                else add(Token.Type.GREATER_THAN, ">");
                break;
            case '&':
                if (next == '&') { tokens.add(new Token(Token.Type.AND, "&&", line)); pos += 2; }
                else { errors.add("Line " + line + ": Unexpected '&'"); add(Token.Type.ERROR, "&"); }
                break;
            case '|':
                if (next == '|') { tokens.add(new Token(Token.Type.OR, "||", line)); pos += 2; }
                else { errors.add("Line " + line + ": Unexpected '|'"); add(Token.Type.ERROR, "|"); }
                break;
            default:
                errors.add("Line " + line + ": Unexpected character '" + c + "'");
                add(Token.Type.ERROR, String.valueOf(c));
        }
    }

    private void add(Token.Type t, String v) { tokens.add(new Token(t, v, line)); pos++; }
    public List<String> getErrors() { return errors; }
}
