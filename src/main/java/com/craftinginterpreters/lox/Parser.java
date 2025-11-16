package main.java.com.craftinginterpreters.lox;

import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;


public class Parser {
    // 6.1: Exceção customizada para erros de parsing
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0; // Ponteiro para o token atual

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null; 
        }
    }

    private Expr expression() {
        // A gramática de expressões de Lox começa no nível mais baixo: 'equality'
        return equality();
    }
    
    // --- Métodos de Precedência (PREENCHIDOS) ---
    
    // 6.2: Equality (== e !=)
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    
    // 6.3: Comparison (> >= < <=)
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    
    // 6.4: Term (+ -)
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    
    // 6.5: Factor (* /)
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    
    // 6.6: Unary (! -)
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }
    
    // 6.7: Primary (Literais, Agrupamento)
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        // Agrupamento
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Esperado ')' depois da expressão.");
            return new Expr.Grouping(expr);
        }
        
        throw error(peek(), "Esperado expressão.");
    }

    // --- Funções Auxiliares de Consumo de Tokens (PREENCHIDAS) ---
    
    // Garante que o token atual seja do tipo esperado e o consome.
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        
        throw error(peek(), message);
    }

    // Verifica se o token atual é de algum dos tipos listados. Se sim, consome.
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    // Verifica se o token atual é de um determinado tipo sem consumi-lo.
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    // Consome o token atual e retorna o token anterior (avançado).
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    // Verifica se chegou ao fim da lista (token EOF)
    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    // Retorna o token na posição 'current' sem consumi-lo.
    private Token peek() {
        return tokens.get(current);
    }

    // Retorna o token consumido antes do token 'current'.
    private Token previous() {
        return tokens.get(current - 1);
    }

    // Reporta um erro de parsing e retorna a exceção ParseError.
    private ParseError error(Token token, String message) {
        Lox.error(token.line, message);
        return new ParseError();
    }
    
    // Discarda tokens após um erro de parsing para tentar sincronizar.
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;

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