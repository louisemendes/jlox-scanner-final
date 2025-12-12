package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Parser (Analisador Sintático)
 *
 * Constrói a Árvore de Sintaxe Abstrata (AST) a partir da lista de tokens produzida
 * pelo Scanner. Implementa a técnica de "Recursive Descent Parsing".
 *
 * Referências Acadêmicas:
 * - Crafting Interpreters, Cap. 6 — Parsing Expressions
 * - Crafting Interpreters, Cap. 8 — Statements and State
 * - Crafting Interpreters, Cap. 9 — Control Flow
 * - Crafting Interpreters, Cap. 10 — Functions
 * - Crafting Interpreters, Cap. 12 — Classes
 *
 * Este parser segue exatamente as gramáticas definidas nos capítulos acima,
 * traduzindo cada regra em uma função recursiva correspondente.
 */
public class Parser {

    /** Classe auxiliar usada para encerrar recursões devido a erros de sintaxe. */
    private static class ParseError extends RuntimeException {}

    /** Limite padrão definido no livro para quantidade de parâmetros e argumentos. */
    private static final int MAX_PARAMETERS = 255;

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // =========================================================================
    //  PARSING PRINCIPAL — PROGRAMA
    // =========================================================================

    /**
     * Ponto de entrada do parser.
     *
     * Referência:
     * Crafting Interpreters — Cap. 8 (Statements)
     * Gramática:
     *   program → declaration* EOF
     */
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            Stmt decl = declaration();
            if (decl != null) {
                statements.add(decl);
            }
        }
        return statements;
    }


    // =========================================================================
    //  DECLARAÇÕES (VAR, FUN, CLASS)
    // =========================================================================

    /**
     * Referência:
     * Crafting Interpreters — Cap. 8 (Statements)
     * Seção: Declarations
     * Gramática:
     *   declaration → classDecl | funDecl | varDecl | statement
     *
     * Inclui tratamento de erros e "panic mode recovery".
     */
    private Stmt declaration() {
        try {
            if (match(CLASS)) return classDeclaration();
            if (match(FUN))   return function("function");
            if (match(VAR))   return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 12 (Classes)
     * Seção: Class Declarations
     * Gramática:
     *   classDecl → "class" IDENTIFIER "{" function* "}"
     */
    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name.");
        consume(LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.");
        return new Stmt.Class(name, methods);
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 10 (Functions)
     * Seção: Function Declarations
     * Gramática:
     *   funDecl → "fun" function
     *   function → IDENTIFIER "(" parameters? ")" block
     */
    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");

        List<Token> parameters = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= MAX_PARAMETERS) {
                    error(peek(), "Can't have more than " + MAX_PARAMETERS + " parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }

        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");

        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 8 (Statements and State)
     * Seção: Variable Declarations
     * Gramática:
     *   varDecl → "var" IDENTIFIER ( "=" expression )? ";"
     */
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) initializer = expression();

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }


    // =========================================================================
    //  STATEMENTS (IF, WHILE, FOR, PRINT, RETURN, BLOCK)
    // =========================================================================

    /**
     * Referência:
     * Crafting Interpreters — Cap. 8, 9 e 10.
     * Gramática:
     *   statement → exprStmt
     *             | ifStmt
     *             | printStmt
     *             | whileStmt
     *             | forStmt
     *             | returnStmt
     *             | block
     */
    private Stmt statement() {
        if (match(FOR))    return forStatement();
        if (match(IF))     return ifStatement();
        if (match(PRINT))  return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE))  return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 10 (Functions)
     * Gramática:
     *   returnStmt → "return" expression? ";"
     */
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;

        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 9 (Control Flow)
     * Seção: For Loops
     * Gramática:
     *   forStmt → "for" "(" ( varDecl | exprStmt | ";" )
     *                        expression? ";"
     *                        expression? ")" statement
     *
     * Observação: O "for" é traduzido internamente (desugared) para um "while".
     */
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }

        consume(RIGHT_PAREN, "Expect ')' after for clauses.");
        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(
                body,
                new Stmt.Expression(increment)
            ));
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 9 (Control Flow)
     * Seção: If Statements
     * Gramática:
     *   ifStmt → "if" "(" expression ")" statement ( "else" statement )?
     */
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;

        if (match(ELSE)) elseBranch = statement();

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 8
     * Seção: Print
     * Gramática:
     *   printStmt → "print" expression ";"
     */
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 9 (Control Flow)
     * Seção: While Loops
     * Gramática:
     *   whileStmt → "while" "(" expression ")" statement
     */
    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");

        return new Stmt.While(condition, statement());
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 8 (Statements)
     * Seção: Blocks and Scope
     * Gramática:
     *   block → "{" declaration* "}"
     */
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            Stmt decl = declaration();
            if (decl != null) statements.add(decl);
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 8 (Statements)
     * Gramática:
     *   exprStmt → expression ";"
     */
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }


    // =========================================================================
    //  EXPRESSÕES (RECURSIVE DESCENT — PRECEDÊNCIA)
    // =========================================================================

    /**
     * Referência:
     * Crafting Interpreters — Cap. 6 (Parsing Expressions)
     * Gramática:
     *   expression → assignment
     */
    private Expr expression() {
        return assignment();
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 8 e Cap. 12
     * Gramática:
     *   assignment → ( call "." IDENTIFIER "=" assignment )
     *               | IDENTIFIER "=" assignment
     *               | logic_or
     */
    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable var) {
                return new Expr.Assign(var.name, value);
            } else if (expr instanceof Expr.Get get) {
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 9 (Control Flow)
     * Gramática:
     *   logic_or → logic_and ( "or" logic_and )*
     */
    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 9
     * Gramática:
     *   logic_and → equality ( "and" equality )*
     */
    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 6
     * Gramática:
     *   equality → comparison ( ( "!=" | "==" ) comparison )*
     */
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 6
     * Gramática:
     *   comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )*
     */
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 6
     * Gramática:
     *   term → factor ( ( "-" | "+" ) factor )*
     */
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 6
     * Gramática:
     *   factor → unary ( ( "/" | "*" ) unary )*
     */
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 6
     * Gramática:
     *   unary → ( "!" | "-" ) unary | call
     */
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 10 (Functions), Cap. 12 (Classes)
     * Gramática:
     *   call → primary ( "(" arguments? ")" | "." IDENTIFIER )*
     */
    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 10
     * Gramática:
     *   arguments → expression ( "," expression )*
     */
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= MAX_PARAMETERS) {
                    error(peek(), "Can't have more than " + MAX_PARAMETERS + " arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 6
     * Gramática:
     *   primary → NUMBER | STRING | "true" | "false" | "nil"
     *            | IDENTIFIER
     *            | "(" expression ")"
     *            | "this"
     */
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE))  return new Expr.Literal(true);
        if (match(NIL))   return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(THIS)) return new Expr.This(previous());

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }


    // =========================================================================
    //  AUXILIARES — MATCH, CONSUME, ERROS E SINCRONIZAÇÃO
    // =========================================================================

    /**
     * Consome o token se ele corresponder a algum dos tipos dados.
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * Consome o token do tipo esperado ou lança erro.
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * Referência:
     * Crafting Interpreters — Cap. 6.4 (Error Recovery)
     * Técnica: Panic Mode
     */
    private ParseError error(Token token, String message) {
        Lox.error(token.line, message);
        return new ParseError();
    }

    /**
     * Sincronização após erros de sintaxe.
     * Avança até um ponto seguro para continuar o parsing.
     *
     * Referência:
     * Crafting Interpreters — Cap. 6.4
     */
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }
}
