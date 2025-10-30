package com.craftinginterpreters.lox;

class Token {
    final TokenType type; // O tipo do token
    final String lexeme;  // O texto exato no código-fonte
    final Object literal; // O valor literal (para strings e números)
    final int line;       // A linha onde o token aparece

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}