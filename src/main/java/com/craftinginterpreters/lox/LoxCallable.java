package com.craftinginterpreters.lox;

import java.util.List;

/**
 * Interface LoxCallable.
 * Define o contrato para qualquer objeto em Lox que possa ser invocado (chamado) como uma função.
 * <p>
 * Isso inclui:
 * 1. Funções nativas (definidas em Java, ex: clock).
 * 2. Funções definidas pelo usuário (LoxFunction).
 * 3. Classes (LoxClass) - que são chamadas para instanciar objetos.
 *
 * Referência: Crafting Interpreters - Capítulo 10 (Functions).
 */
interface LoxCallable {

    /**
     * Retorna o número de argumentos que o chamável espera (aridade).
     * O interpretador verifica isso antes de chamar o método 'call' para garantir
     * que o usuário passou a quantidade correta de parâmetros.
     */
    int arity();

    /**
     * Executa a lógica do objeto chamável.
     *
     * @param interpreter A instância do interpretador (necessária para executar blocos de código ou acessar ambiente).
     * @param arguments   A lista de valores avaliados passados como argumentos.
     * @return O resultado da execução (ou null se for void, ou a nova instância se for uma classe).
     */
    Object call(Interpreter interpreter, List<Object> arguments);
}