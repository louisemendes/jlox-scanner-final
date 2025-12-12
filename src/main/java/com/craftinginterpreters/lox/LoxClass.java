package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

/**
 * LoxClass.
 * Referência: Crafting Interpreters - Capítulo 12 (Classes).
 * Representa a definição de uma classe em tempo de execução.
 * Classes em Lox são "chamáveis" (LoxCallable) para criar instâncias.
 */
class LoxClass implements LoxCallable {
    final String name;

    LoxClass(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Quando a classe é "chamada" (ex: Pessoa()), criamos uma nova instância.
     */
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return new LoxInstance(this);
    }

    /**
     * Por enquanto, aridade é 0 (sem construtor com argumentos).
     */
    @Override
    public int arity() {
        return 0;
    }
}
