package com.craftinginterpreters.lox;

/**
 * Representa um token individual processado pelo Scanner.
 * Um token é a menor unidade de significado no código-fonte.
 * * Referência: Crafting Interpreters - Capítulo 4.2 (Representing Code).
 */
class Token {
    // [Livro 4.2] Tipo categórico do token (ex: NUMBER ou PLUS)
    final TokenType type;

    // [Livro 4.2] O texto exato (string crua) que estava no código-fonte.
    // Útil para mensagens de erro, para mostrar exatamente o que o usuário digitou.
    final String lexeme;

    // [Livro 4.2.2] O valor interpretado do token (apenas para literais).
    // Ex: Se o lexeme for "123", o literal será o Double 123.0.
    // Para keywords ou pontuação, este valor é null.
    final Object literal;

    // [Livro 4.2.3] O número da linha onde o token foi encontrado.
    // Fundamental para indicar ao usuário onde ocorreram erros.
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    /**
     * Retorna uma representação em String do token.
     * Formato: TIPO LEXEMA LITERAL
     * Exemplo: NUMBER 123 123.0
     */
    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}