package com.craftinginterpreters.lox;

/**
 * RuntimeError (Erro de Tempo de Execução).
 * Exceção personalizada lançada pelo Interpreter quando o código é sintaticamente válido,
 * mas executa uma operação ilegal (ex: divisão por zero, somar número com texto).
 *
 * Esta classe permite que o interpretador capture o erro, mostre a linha correta
 * e continue rodando (no caso do REPL) ou termine graciosamente.
 *
 * Referência: Crafting Interpreters - Capítulo 7 (Evaluating Expressions).
 */
class RuntimeError extends RuntimeException {
    
    // O token onde o erro ocorreu. 
    // Usado para informar ao usuário o número da linha exata do problema.
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}