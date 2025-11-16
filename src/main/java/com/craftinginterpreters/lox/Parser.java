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
    
    // --- Métodos de Precedência (A serem preenchidos) ---
    private Expr equality() { /* ... */ return comparison(); }
    private Expr comparison() { /* ... */ return term(); }
    private Expr term() { /* ... */ return factor(); }
    private Expr factor() { /* ... */ return unary(); }
    private Expr unary() { /* ... */ return primary(); }
    private Expr primary() { /* ... */ return null; }

    // --- Funções Auxiliares de Consumo de Tokens (A serem preenchidos) ---
    private Token consume(TokenType type, String message) { /* ... */ return null; }
    private boolean match(TokenType... types) { /* ... */ return false; }
    private boolean check(TokenType type) { /* ... */ return false; }
    private Token advance() { /* ... */ return null; }
    private boolean isAtEnd() { /* ... */ return false; }
    private Token peek() { /* ... */ return null; }
    private Token previous() { /* ... */ return null; }
    private ParseError error(Token token, String message) { /* ... */ return new ParseError(); }
    private void synchronize() { /* ... */ }
}