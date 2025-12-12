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
        LoxInstance instance = new LoxInstance(this);

        // [Cap. 12 - ATUALIZADO] Busca pelo inicializador (construtor) "init".
        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            // Se existir, vincula 'this' ao novo objeto e executa a função imediatamente.
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override
    public int arity() {
        // [Cap. 12 - ATUALIZADO] Se houver um inicializador, a aridade da classe é a dele.
        // Se não houver, a aridade é 0 (construtor padrão sem argumentos).
        LoxFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }
}