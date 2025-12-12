package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Representa uma instância concreta (objeto) de uma LoxClass.
 *
 * Referência: Crafting Interpreters – Capítulo 12 (Classes).
 *
 * Cada instância possui:
 *  - Um conjunto de campos próprios (fields).
 *  - Uma referência à sua classe, responsável por fornecer os métodos.
 */
class LoxInstance {

    /**
     * Classe da qual esta instância foi criada.
     * Imutável após construção.
     */
    private final LoxClass klass;

    /**
     * Campos da instância.
     *
     * Conforme o livro (cap. 12), instâncias podem ter campos adicionados
     * dinamicamente via atribuição, mesmo que não tenham sido declarados antes.
     */
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    /**
     * Resgata um campo ou método associado ao nome solicitado.
     *
     * Ordem conforme o livro:
     *  1. Procura primeiro em fields (propriedades dinâmicas).
     *  2. Se inexistente, procura método na classe (e nas superclasses, cap. 13).
     *  3. Caso não exista nada, lança erro.
     */
    Object get(Token name) {
        // 1. Propriedade (campo) definido na instância
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        // 2. Método definido na classe
        LoxFunction method = klass.findMethod(name.lexeme);
        if (method != null) {
            // Vincula o método à instância, criando o ambiente com "this"
            return method.bind(this);
        }

        // 3. Nada encontrado
        throw new RuntimeError(name,
                "Undefined property '" + name.lexeme + "'.");
    }

    /**
     * Define ou sobrescreve um campo na instância.
     * Em Lox, campos podem ser criados dinamicamente.
     */
    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    /**
     * Representação textual da instância.
     * Usado no capítulo 12 para facilitar o debugging e o print de objetos.
     */
    @Override
    public String toString() {
        return this.klass.name + " instance";
    }
}
