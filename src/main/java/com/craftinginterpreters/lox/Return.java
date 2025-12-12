package com.craftinginterpreters.lox;

/**
 * Return (Exceção de Controle de Fluxo).
 * Em Lox (interpretado em Java), usamos uma exceção para "saltar" de volta do corpo
 * de uma função até o ponto onde ela foi chamada (no Interpreter).
 * <p>
 * Isso não representa um erro, mas sim o comportamento do comando 'return'.
 *
 * Referência: Crafting Interpreters - Capítulo 10 (Functions).
 */
class Return extends RuntimeException {
    
    // [Cap. 10] O valor que a função está retornando.
    final Object value;

    Return(Object value) {
        // [Cap. 10] Otimização JVM:
        // Chamamos o construtor de RuntimeException desabilitando a geração de Stack Trace.
        // Como usamos essa exceção para controle de fluxo frequente, não queremos o custo
        // de desempenho de capturar a pilha de execução toda vez.
        super(null, null, false, false);
        this.value = value;
    }
}
