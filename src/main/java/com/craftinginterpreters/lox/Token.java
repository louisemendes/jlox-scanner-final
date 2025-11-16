package com.craftinginterpreters.lox;

class Token {
    // 4.2.1: Tipo do token (ex: TokenType.NUMBER)
    final TokenType type;
    // 4.2.1: Lexema - O texto exato no código-fonte (ex: "123.45")
    final String lexeme;
    // 4.2.2: Literal - O valor em tempo de execução (ex: 123.45 do tipo Double)
    final Object literal;
    // 4.2.3: Linha - Para relatórios de erro
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}