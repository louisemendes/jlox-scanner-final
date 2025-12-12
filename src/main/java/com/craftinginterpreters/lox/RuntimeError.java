package com.craftinginterpreters.lox;

/**
 * RuntimeError (Erro de Tempo de Execução).
 * Exceção personalizada lançada pelo Interpreter quando o código é sintaticamente válido,
 * mas executa uma operação ilegal (ex: divisão por zero, acessar propriedade de null).
 * <p>
 * Diferente de erros de sintaxe (que impedem o programa de rodar), estes ocorrem
 * durante a execução. Esta classe carrega o token para reportar a linha exata do erro.
 *
 * Referência: Crafting Interpreters - Capítulo 7 (Evaluating Expressions).
 */
class RuntimeError extends RuntimeException {

    // [Cap. 7] O token onde o erro ocorreu.
    // Essencial para informar ao usuário a linha exata onde a operação falhou.
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}