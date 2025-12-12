package com.craftinginterpreters.lox;

/**
 * Representa um token individual processado pelo Scanner.
 * Um token é a menor unidade de significado (átomo) no código-fonte.
 *
 * Referência: Crafting Interpreters - Capítulo 4.2 (Representing Code).
 */
class Token {
    // [Cap. 4.2] Tipo categórico do token (ex: NUMBER, IDENTIFIER, PLUS).
    final TokenType type;

    // [Cap. 4.2] O texto exato (string crua) extraído do código-fonte.
    // Essencial para mensagens de erro, permitindo mostrar exatamente o que o usuário digitou.
    final String lexeme;

    // [Cap. 4.2.2] O valor semântico do token (apenas para literais).
    // Ex: Se o lexeme for "123", o literal será um objeto Double com valor 123.0.
    // Para palavras-chave ou pontuação, este valor permanece null.
    final Object literal;

    // [Cap. 4.2.3] O número da linha onde o token foi encontrado.
    // Fundamental para o tratamento de erros, indicando a localização do problema.
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    /**
     * Retorna uma representação em String do token para depuração.
     * Formato: TIPO LEXEMA LITERAL
     * Exemplo: NUMBER 123 123.0
     */
    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}