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

    LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}
