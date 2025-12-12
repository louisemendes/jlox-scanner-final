package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * LoxInstance.
 * Referência: Crafting Interpreters - Capítulo 12 (Classes).
 * Representa uma instância concreta (objeto) de uma classe Lox.
 */
class LoxInstance {
    private LoxClass klass;
    
    // [Cap. 12] Mapa de campos (propriedades) da instância.
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    // [Cap. 12] Busca uma propriedade ou método.
    Object get(Token name) {
        // 1. Tenta buscar campo (propriedade) na instância
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        // 2. Se não achou campo, tenta buscar MÉTODO na classe
        LoxFunction method = klass.findMethod(name.lexeme);
        if (method != null) return method;

        // Se não achou nada, erro.
        throw new RuntimeError(name, 
            "Undefined property '" + name.lexeme + "'.");
    }

    // [Cap. 12] Define uma propriedade.
    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}
