package com.craftinginterpreters.lox;

/**
 * Enumeração que define todos os tipos de tokens (lexemas) suportados pela linguagem Lox.
 * * Referência: Crafting Interpreters - Capítulo 4 (Scanning).
 * <p>
 * Os tokens são divididos em categorias funcionais:
 * 1. Caractere único (pontuação/operadores simples).
 * 2. Um ou dois caracteres (operadores de comparação e lógica).
 * 3. Literais (valores definidos pelo usuário como números e strings).
 * 4. Palavras-chave (reservadas pela linguagem).
 * 5. Controle (EOF).
 */
enum TokenType {
    // --- [Cap. 4.2.1] Tokens de caractere único ---
    // Podem ser reconhecidos imediatamente ao ler o caractere.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    // --- [Cap. 4.2.1] Tokens de um ou dois caracteres ---
    // O Scanner precisa verificar o próximo caractere (lookahead) para decidir (ex: '=' vs '==').
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // --- [Cap. 4.2.1] Literais ---
    // Tokens que carregam valores variáveis definidos no código fonte.
    IDENTIFIER, // Ex: nomeVariavel, NomeClasse
    STRING,     // Ex: "Olá Mundo"
    NUMBER,     // Ex: 123, 45.67

    // --- [Cap. 4.2.1] Palavras-chave (Keywords) ---
    // Identificadores reservados que estruturam a linguagem.
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    // --- [Cap. 4.4] Controle ---
    EOF // End Of File (Fim do Arquivo) - Sinaliza o término da análise.
}