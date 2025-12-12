package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

/**
 * LoxClass.
 * Referência: Crafting Interpreters - Capítulo 12 (Classes).
 * Representa a definição de uma classe em tempo de execução.
 */
class LoxClass implements LoxCallable {
    final String name;
    // [Cap. 12] Mapa contendo os métodos da classe (nome -> função)
    private final Map<String, LoxFunction> methods;

    LoxClass(String name, Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    /**
     * [Cap. 12] Busca um método pelo nome na definição da classe.
     */
    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return new LoxInstance(this);
    }

    @Override
    public int arity() {
        return 0;
    }
}