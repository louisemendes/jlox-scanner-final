package com.craftinginterpreters.lox;

/**
 * Erro de tempo de execução do interpretador.
 * Guarda o token que gerou o erro para reportar localização.
 */
public class RuntimeError extends RuntimeException {
    final Token token;

    public RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}