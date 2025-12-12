package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Environment (Ambiente de Execução).
 * Armazena as variáveis e seus valores durante a execução do programa.
 * Implementa o conceito de escopo léxico através de encadeamento de ambientes.
 *
 * Referência: Crafting Interpreters - Capítulo 8 (Statements and State).
 */
class Environment {
    
    // [Cap. 8] Referência ao ambiente que envolve este (Escopo Pai).
    // Se for nulo, significa que este é o ambiente global.
    // Isso permite que um bloco acesse variáveis definidas fora dele (Closure).
    final Environment enclosing;

    // Mapa para armazenar os nomes das variáveis e seus valores neste escopo.
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
     * [Cap. 11] Navega pela cadeia de ambientes.
     * Retorna o ambiente que está a uma exata "distância" (número de saltos) do atual.
     * Usado pelo Resolver para encontrar onde a variável foi declarada de forma determinística.
     */
    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }
        return environment;
    }

    /**
     * [Cap. 11] Busca o valor de uma variável em uma distância específica.
     * O Resolver garante que a variável existe nesse local.
     */
    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    /**
     * [Cap. 11] Atribui valor a uma variável em uma distância específica.
     */
    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    /**
     * [Cap. 8] Busca o valor de uma variável (Lookup Dinâmico).
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
     * [Cap. 8] Atribui um novo valor a uma variável existente (Atribuição Dinâmica).
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