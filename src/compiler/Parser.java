package compiler;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 2 — Syntax Analyzer (Recursive Descent Parser).
 * Consumes a token stream and builds an Abstract Syntax Tree.
 * Supports C-like constructs: for, return, printf, char, string literals,
 * comma-separated declarations, compound assignments, single-statement bodies.
 *
 * Grammar (simplified C subset):
 *   program       -> (preprocessor | functionDef | statement)*
 *   functionDef   -> type IDENTIFIER '(' params? ')' block
 *   statement     -> varDecl | assignment | ifStmt | whileStmt | forStmt
 *                  | printStmt | printfStmt | returnStmt | block | exprStmt | ';'
 *   varDecl       -> type declList ';'
 *   declList      -> declItem (',' declItem)*
 *   declItem      -> IDENTIFIER ('=' expression)?
 *   assignment    -> IDENTIFIER '=' expression ';'
 *                  | IDENTIFIER compoundOp expression ';'
 *                  | IDENTIFIER '++' ';'  |  IDENTIFIER '--' ';'
 *   ifStmt        -> 'if' '(' expression ')' stmtOrBlock ('else' stmtOrBlock)?
 *   whileStmt     -> 'while' '(' expression ')' stmtOrBlock
 *   forStmt       -> 'for' '(' forInit? ';' expression? ';' forUpdate? ')' stmtOrBlock
 *   printStmt     -> 'print' '(' expression ')' ';'
 *   printfStmt    -> 'printf' '(' STRING (',' expression)* ')' ';'
 *   returnStmt    -> 'return' expression? ';'
 *   stmtOrBlock   -> block | statement
 *   expression    -> logicalOr
 *   primary       -> NUMBER | IDENTIFIER ('++' | '--')? | STRING | CHAR | '(' expression ')'
 */
public class Parser {
    private final List<Token> tokens;
    private int pos = 0;
    private final List<String> errors = new ArrayList<>();

    public Parser(List<Token> tokens) { this.tokens = tokens; }
    public List<String> getErrors() { return errors; }

    /* -- helpers ------------------------------------------------ */
    private Token current() { return tokens.get(pos); }
    private boolean check(Token.Type t) { return current().getType() == t; }
    private Token advance() { Token t = current(); pos++; return t; }

    private Token expect(Token.Type t, String msg) {
        if (check(t)) return advance();
        throw new RuntimeException("Line " + current().getLine() + ": " + msg + " (got '" + current().getValue() + "')");
    }

    /** Panic-mode recovery: skip tokens until a statement boundary */
    private void synchronize() {
        while (!check(Token.Type.EOF)) {
            if (check(Token.Type.SEMICOLON)) { advance(); return; }
            if (check(Token.Type.INT) || check(Token.Type.FLOAT) || check(Token.Type.CHAR)
                || check(Token.Type.IF) || check(Token.Type.WHILE) || check(Token.Type.FOR)
                || check(Token.Type.PRINT) || check(Token.Type.PRINTF) || check(Token.Type.RETURN)
                || check(Token.Type.RBRACE)) return;
            advance();
        }
    }

    private boolean isTypeKeyword() {
        return check(Token.Type.INT) || check(Token.Type.FLOAT) || check(Token.Type.CHAR);
    }

    /* -- entry -------------------------------------------------- */
    public ASTNode parse() {
        ASTNode prog = ASTNode.program();
        while (!check(Token.Type.EOF)) {
            try {
                // Skip preprocessor directives
                if (check(Token.Type.PREPROCESSOR)) {
                    prog.addChild(ASTNode.preprocessor(advance().getValue()));
                    continue;
                }
                // Check for function definition: type IDENTIFIER '(' ...
                if (isTypeKeyword() && pos + 1 < tokens.size()
                    && tokens.get(pos + 1).getType() == Token.Type.IDENTIFIER
                    && pos + 2 < tokens.size()
                    && tokens.get(pos + 2).getType() == Token.Type.LPAREN) {
                    prog.addChild(parseFunctionDef());
                } else {
                    prog.addChild(parseStatement());
                }
            } catch (RuntimeException e) {
                errors.add(e.getMessage());
                synchronize();
            }
        }
        return prog;
    }

    /* -- function definition ------------------------------------ */
    private ASTNode parseFunctionDef() {
        String retType = advance().getValue();  // type
        String funcName = expect(Token.Type.IDENTIFIER, "Expected function name").getValue();
        expect(Token.Type.LPAREN, "Expected '('");
        // Skip parameters (we don't deeply parse param types for now)
        while (!check(Token.Type.RPAREN) && !check(Token.Type.EOF)) advance();
        expect(Token.Type.RPAREN, "Expected ')'");
        ASTNode body = parseBlock();
        ASTNode func = ASTNode.functionDef(retType, funcName);
        func.addChild(body);
        return func;
    }

    /* -- statements --------------------------------------------- */
    private ASTNode parseStatement() {
        if (isTypeKeyword()) return parseVarDecl();
        if (check(Token.Type.IF))     return parseIf();
        if (check(Token.Type.WHILE))  return parseWhile();
        if (check(Token.Type.FOR))    return parseFor();
        if (check(Token.Type.PRINT))  return parsePrint();
        if (check(Token.Type.PRINTF)) return parsePrintf();
        if (check(Token.Type.RETURN)) return parseReturn();
        if (check(Token.Type.LBRACE)) return parseBlock();
        if (check(Token.Type.SEMICOLON)) { advance(); return ASTNode.emptyStmt(); }
        if (check(Token.Type.IDENTIFIER)) return parseIdentifierStatement();
        throw new RuntimeException("Line " + current().getLine() + ": Unexpected token '" + current().getValue() + "'");
    }

    /** Parse a statement or a block (for if/else/while/for bodies without braces) */
    private ASTNode parseStmtOrBlock() {
        if (check(Token.Type.LBRACE)) return parseBlock();
        // Single statement — wrap in a block for consistency
        ASTNode blk = ASTNode.block();
        blk.addChild(parseStatement());
        return blk;
    }

    /* -- variable declaration: type declItem (',' declItem)* ';' -- */
    private ASTNode parseVarDecl() {
        String dt = advance().getValue(); // int / float / char
        // First declaration
        ASTNode firstDecl = parseDeclItem(dt);
        // Check for comma-separated additional declarations
        List<ASTNode> decls = new ArrayList<>();
        decls.add(firstDecl);
        while (check(Token.Type.COMMA)) {
            advance(); // skip ','
            decls.add(parseDeclItem(dt));
        }
        expect(Token.Type.SEMICOLON, "Expected ';'");
        // If only one decl, return it directly; otherwise wrap in a block
        if (decls.size() == 1) return decls.get(0);
        ASTNode blk = ASTNode.block();
        for (ASTNode d : decls) blk.addChild(d);
        return blk;
    }

    /** Parse a single declaration item: IDENTIFIER ('=' expression)? */
    private ASTNode parseDeclItem(String dataType) {
        Token id = expect(Token.Type.IDENTIFIER, "Expected variable name");
        ASTNode n = ASTNode.varDecl(dataType, id.getValue());
        if (check(Token.Type.ASSIGN)) {
            advance(); // skip '='
            n.addChild(parseExpression());
        } else {
            // Uninitialized — add a default value (0 for int/float, '\0' for char)
            if ("char".equals(dataType)) {
                n.addChild(ASTNode.charLiteral("'\\0'"));
            } else {
                n.addChild(ASTNode.intLiteral("0"));
            }
        }
        return n;
    }

    /* -- identifier-led statements: assign, compound assign, i++, i-- */
    private ASTNode parseIdentifierStatement() {
        Token id = advance();
        // i++ ;
        if (check(Token.Type.INCREMENT)) {
            advance();
            expect(Token.Type.SEMICOLON, "Expected ';'");
            return ASTNode.postIncrement(id.getValue());
        }
        // i-- ;
        if (check(Token.Type.DECREMENT)) {
            advance();
            expect(Token.Type.SEMICOLON, "Expected ';'");
            return ASTNode.postDecrement(id.getValue());
        }
        // Compound assignment: += -= *= /=
        if (check(Token.Type.PLUS_ASSIGN) || check(Token.Type.MINUS_ASSIGN)
            || check(Token.Type.MULTIPLY_ASSIGN) || check(Token.Type.DIVIDE_ASSIGN)) {
            String op = advance().getValue();
            ASTNode expr = parseExpression();
            expect(Token.Type.SEMICOLON, "Expected ';'");
            ASTNode n = ASTNode.compoundAssign(id.getValue(), op);
            n.addChild(expr);
            return n;
        }
        // Simple assignment: =
        expect(Token.Type.ASSIGN, "Expected '=', '++', '--', or compound assignment");
        ASTNode expr = parseExpression();
        expect(Token.Type.SEMICOLON, "Expected ';'");
        ASTNode n = ASTNode.assign(id.getValue());
        n.addChild(expr);
        return n;
    }

    /* -- if statement ------------------------------------------- */
    private ASTNode parseIf() {
        advance(); // skip 'if'
        expect(Token.Type.LPAREN, "Expected '('");
        ASTNode cond = parseExpression();
        expect(Token.Type.RPAREN, "Expected ')'");
        ASTNode then = parseStmtOrBlock();
        ASTNode n = ASTNode.ifNode();
        n.addChild(cond);
        n.addChild(then);
        if (check(Token.Type.ELSE)) {
            advance();
            // else if (...) is handled naturally because parseStmtOrBlock will parse the if
            n.addChild(parseStmtOrBlock());
        }
        return n;
    }

    /* -- while statement ---------------------------------------- */
    private ASTNode parseWhile() {
        advance(); // skip 'while'
        expect(Token.Type.LPAREN, "Expected '('");
        ASTNode cond = parseExpression();
        expect(Token.Type.RPAREN, "Expected ')'");
        ASTNode body = parseStmtOrBlock();
        ASTNode n = ASTNode.whileNode();
        n.addChild(cond);
        n.addChild(body);
        return n;
    }

    /* -- for statement ------------------------------------------ */
    private ASTNode parseFor() {
        advance(); // skip 'for'
        expect(Token.Type.LPAREN, "Expected '('");

        // Init part (can be varDecl, assignment, i++, or empty)
        ASTNode init;
        if (check(Token.Type.SEMICOLON)) {
            init = ASTNode.emptyStmt();
            advance();
        } else if (isTypeKeyword()) {
            init = parseVarDecl(); // this already consumes ';'
        } else {
            init = parseForExprStmt();
            expect(Token.Type.SEMICOLON, "Expected ';' after for-init");
        }

        // Condition
        ASTNode cond;
        if (check(Token.Type.SEMICOLON)) {
            cond = ASTNode.intLiteral("1"); // infinite loop
        } else {
            cond = parseExpression();
        }
        expect(Token.Type.SEMICOLON, "Expected ';' after for-condition");

        // Update part
        ASTNode update;
        if (check(Token.Type.RPAREN)) {
            update = ASTNode.emptyStmt();
        } else {
            update = parseForExprStmt();
        }
        expect(Token.Type.RPAREN, "Expected ')'");

        ASTNode body = parseStmtOrBlock();

        ASTNode n = ASTNode.forNode();
        n.addChild(init);
        n.addChild(cond);
        n.addChild(update);
        n.addChild(body);
        return n;
    }

    /** Parse a for-loop init/update expression (assignment, i++, i--) without consuming ';' */
    private ASTNode parseForExprStmt() {
        if (check(Token.Type.IDENTIFIER)) {
            Token id = advance();
            if (check(Token.Type.INCREMENT)) { advance(); return ASTNode.postIncrement(id.getValue()); }
            if (check(Token.Type.DECREMENT)) { advance(); return ASTNode.postDecrement(id.getValue()); }
            if (check(Token.Type.PLUS_ASSIGN) || check(Token.Type.MINUS_ASSIGN)
                || check(Token.Type.MULTIPLY_ASSIGN) || check(Token.Type.DIVIDE_ASSIGN)) {
                String op = advance().getValue();
                ASTNode expr = parseExpression();
                ASTNode n = ASTNode.compoundAssign(id.getValue(), op);
                n.addChild(expr);
                return n;
            }
            if (check(Token.Type.ASSIGN)) {
                advance();
                ASTNode expr = parseExpression();
                ASTNode n = ASTNode.assign(id.getValue());
                n.addChild(expr);
                return n;
            }
            // Just an identifier expression (shouldn't normally happen in for-update)
            return ASTNode.identifier(id.getValue());
        }
        return parseExpression();
    }

    /* -- print/printf/return ------------------------------------ */
    private ASTNode parsePrint() {
        advance(); // skip 'print'
        expect(Token.Type.LPAREN, "Expected '('");
        ASTNode expr = parseExpression();
        expect(Token.Type.RPAREN, "Expected ')'");
        expect(Token.Type.SEMICOLON, "Expected ';'");
        ASTNode n = ASTNode.printNode();
        n.addChild(expr);
        return n;
    }

    private ASTNode parsePrintf() {
        advance(); // skip 'printf'
        expect(Token.Type.LPAREN, "Expected '('");
        // First arg should be a string literal (format string)
        ASTNode fmt;
        if (check(Token.Type.STRING_LITERAL)) {
            fmt = ASTNode.stringLiteral(advance().getValue());
        } else {
            fmt = parseExpression();
        }
        ASTNode n = ASTNode.printfNode();
        n.addChild(fmt);
        // Additional arguments
        while (check(Token.Type.COMMA)) {
            advance();
            n.addChild(parseExpression());
        }
        expect(Token.Type.RPAREN, "Expected ')'");
        expect(Token.Type.SEMICOLON, "Expected ';'");
        return n;
    }

    private ASTNode parseReturn() {
        advance(); // skip 'return'
        ASTNode n = ASTNode.returnNode();
        if (!check(Token.Type.SEMICOLON)) {
            n.addChild(parseExpression());
        }
        expect(Token.Type.SEMICOLON, "Expected ';'");
        return n;
    }

    /* -- block -------------------------------------------------- */
    private ASTNode parseBlock() {
        expect(Token.Type.LBRACE, "Expected '{'");
        ASTNode blk = ASTNode.block();
        while (!check(Token.Type.RBRACE) && !check(Token.Type.EOF)) {
            try {
                blk.addChild(parseStatement());
            } catch (RuntimeException e) {
                errors.add(e.getMessage());
                synchronize();
            }
        }
        expect(Token.Type.RBRACE, "Expected '}'");
        return blk;
    }

    /* -- expressions (precedence climbing) ---------------------- */
    private ASTNode parseExpression() { return parseLogicalOr(); }

    private ASTNode parseLogicalOr() {
        ASTNode left = parseLogicalAnd();
        while (check(Token.Type.OR)) {
            String op = advance().getValue();
            ASTNode n = ASTNode.binaryOp(op); n.addChild(left); n.addChild(parseLogicalAnd()); left = n;
        }
        return left;
    }

    private ASTNode parseLogicalAnd() {
        ASTNode left = parseComparison();
        while (check(Token.Type.AND)) {
            String op = advance().getValue();
            ASTNode n = ASTNode.binaryOp(op); n.addChild(left); n.addChild(parseComparison()); left = n;
        }
        return left;
    }

    private ASTNode parseComparison() {
        ASTNode left = parseAddition();
        if (check(Token.Type.EQUALS) || check(Token.Type.NOT_EQUALS) || check(Token.Type.LESS_THAN)
            || check(Token.Type.GREATER_THAN) || check(Token.Type.LESS_EQUAL) || check(Token.Type.GREATER_EQUAL)) {
            String op = advance().getValue();
            ASTNode n = ASTNode.binaryOp(op); n.addChild(left); n.addChild(parseAddition()); left = n;
        }
        return left;
    }

    private ASTNode parseAddition() {
        ASTNode left = parseMultiplication();
        while (check(Token.Type.PLUS) || check(Token.Type.MINUS)) {
            String op = advance().getValue();
            ASTNode n = ASTNode.binaryOp(op); n.addChild(left); n.addChild(parseMultiplication()); left = n;
        }
        return left;
    }

    private ASTNode parseMultiplication() {
        ASTNode left = parseUnary();
        while (check(Token.Type.MULTIPLY) || check(Token.Type.DIVIDE) || check(Token.Type.MODULO)) {
            String op = advance().getValue();
            ASTNode n = ASTNode.binaryOp(op); n.addChild(left); n.addChild(parseUnary()); left = n;
        }
        return left;
    }

    private ASTNode parseUnary() {
        if (check(Token.Type.MINUS) || check(Token.Type.NOT)) {
            String op = advance().getValue();
            ASTNode n = ASTNode.unaryOp(op); n.addChild(parseUnary()); return n;
        }
        return parsePrimary();
    }

    private ASTNode parsePrimary() {
        if (check(Token.Type.INTEGER_LITERAL)) return ASTNode.intLiteral(advance().getValue());
        if (check(Token.Type.FLOAT_LITERAL))   return ASTNode.floatLiteral(advance().getValue());
        if (check(Token.Type.STRING_LITERAL))  return ASTNode.stringLiteral(advance().getValue());
        if (check(Token.Type.CHAR_LITERAL))    return ASTNode.charLiteral(advance().getValue());
        if (check(Token.Type.IDENTIFIER)) {
            Token id = advance();
            // Post-increment / post-decrement in expressions
            if (check(Token.Type.INCREMENT)) { advance(); return ASTNode.postIncrement(id.getValue()); }
            if (check(Token.Type.DECREMENT)) { advance(); return ASTNode.postDecrement(id.getValue()); }
            return ASTNode.identifier(id.getValue());
        }
        if (check(Token.Type.LPAREN)) {
            advance();
            ASTNode e = parseExpression();
            expect(Token.Type.RPAREN, "Expected ')'");
            return e;
        }
        throw new RuntimeException("Line " + current().getLine() + ": Expected expression, got '" + current().getValue() + "'");
    }
}
