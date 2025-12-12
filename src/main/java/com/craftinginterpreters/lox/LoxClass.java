package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

/**
 * LoxClass.
 * Representa a definição de uma classe em tempo de execução.
 * No Lox, classes são "cidadãos de primeira classe" e implementam LoxCallable
 * porque a própria classe é chamada (ex: Bolo()) para criar instâncias.
 *
 * Referência: Crafting Interpreters - Capítulo 12 (Classes).
 */
class LoxClass implements LoxCallable {
    final String name;
    
    // [Cap. 12] Tabela de métodos da classe.
    // Armazena as definições de função que pertencem a esta classe.
    private final Map<String, LoxFunction> methods;

    LoxClass(String name, Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    /**
     * [Cap. 12] Busca a definição de um método pelo nome.
     * Usado quando uma instância tenta acessar uma propriedade que não é um campo.
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

    /**
     * [Cap. 12] Processo de Instanciação.
     * 1. Cria a instância vazia.
     * 2. Verifica se existe um construtor ("init").
     * 3. Se existir, executa-o vinculando a nova instância.
     * 4. Retorna a instância.
     */
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);

        // Busca pelo inicializador (construtor)
        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            // Vincula 'this' ao novo objeto e executa a lógica de inicialização.
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    /**
     * [Cap. 12] A aridade da classe (número de argumentos) é determinada pelo seu construtor.
     */
    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }
}