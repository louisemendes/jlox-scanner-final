package com.craftinginterpreters.lox;

enum TokenType {
    // 4.1: Tokens de caractere único
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    // 4.1: Tokens de um ou dois caracteres
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // 4.2.1: Literais (Valores)
    IDENTIFIER, STRING, NUMBER,

    // 4.2.1: Palavras-chave (Keywords)
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    // 4.4: Fim do Arquivo
    EOF
}