package com.craftinginterpreters.lox;

/**
 * Enumeração que define todos os tipos de tokens suportados pela linguagem Lox.
 * Referência: Crafting Interpreters - Capítulo 4 (Scanning).
 */
enum TokenType {
    // [Livro 4.1] Tokens de caractere único.
    // São identificados imediatamente, sem necessidade de olhar o próximo caractere.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    // [Livro 4.1] Tokens de um ou dois caracteres.
    // O Scanner precisa olhar à frente (lookahead) para distinguir '=' de '=='.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // [Livro 4.2.1] Literais.
    // Tokens que carregam um valor específico definido pelo usuário.
    IDENTIFIER, // Ex: nomeDaVariavel
    STRING,     // Ex: "texto"
    NUMBER,     // Ex: 123.45

    // [Livro 4.2.1] Palavras-chave (Keywords).
    // Palavras reservadas que controlam a estrutura da linguagem.
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    // [Livro 4.4] Fim do Arquivo.
    // Sinaliza ao Parser que o código acabou.
    EOF
}