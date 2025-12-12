package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Environment (Ambiente de Execução).
 * Armazena as variáveis e seus valores durante a execução do programa.
 * Implementa o conceito de escopo léxico através de encadeamento.
 *
 * Referência: Crafting Interpreters - Capítulo 8 (Statements and State).
 */
class Environment {
    
    // [Cap. 8] Referência ao ambiente que envolve este (Escopo Pai).
    // Se for nulo, significa que este é o ambiente global.
    // Isso permite que um bloco acesse variáveis definidas fora dele.
    final Environment enclosing;

    // Mapa para armazenar os nomes das variáveis e seus valores.
    private final Map<String, Object> values = new HashMap<>();

    /**
     * Construtor para o escopo global (sem pai).
     */
    Environment() {
        enclosing = null;
    }

    /**
     * Construtor para escopos locais (blocos).
     * @param enclosing O ambiente externo onde este novo ambiente está aninhado.
     */
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    /**
     * [Cap. 8] Define uma nova variável.
     * Ao contrário da atribuição, 'define' sempre cria uma nova entrada no escopo atual,
     * mesmo que a variável já exista (permitindo sombreamento/shadowing).
     */
    void define(String name, Object value) {
        values.put(name, value);
    }

    /**
     * [Cap. 8] Busca o valor de uma variável.
     * Se a variável não for encontrada neste escopo, tenta buscar no escopo pai (recursão).
     */
    Object get(Token name) {
        // 1. Tenta encontrar no escopo atual
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        // 2. Se não achou e existe um pai, delega a busca para o pai
        if (enclosing != null) return enclosing.get(name);

        // 3. Se chegou ao topo (global) e não achou, é um erro de tempo de execução.
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    /**
     * [Cap. 8] Atribui um novo valor a uma variável existente.
     * Não é permitido criar variáveis novas via atribuição (deve-se usar 'var' para isso).
     */
    void assign(Token name, Object value) {
        // 1. Se a variável existe neste escopo, atualiza o valor
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        // 2. Se não existe aqui, tenta atribuir no escopo pai
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        // 3. Se ninguém tem essa variável, erro.
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}