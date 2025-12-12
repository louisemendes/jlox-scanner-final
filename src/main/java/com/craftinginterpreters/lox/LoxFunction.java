package com.craftinginterpreters.lox;

import java.util.List;

/**
 * LoxFunction.
 * Representa funções definidas pelo usuário, métodos de classe e construtores.
 * Funciona como um wrapper em torno da declaração sintática (Stmt.Function) e
 * do ambiente em que foi criada (Closure).
 *
 * Referência: Crafting Interpreters - Capítulo 10 (Functions) e 12 (Classes).
 */
class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    
    // [Cap. 10] Closure: O ambiente que estava ativo quando a função foi declarada.
    // Para métodos, este ambiente inclui o "this" vinculado à instância.
    private final Environment closure;

    // [Cap. 12] Indica se esta função é um inicializador (construtor "init").
    private final boolean isInitializer;

    LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.isInitializer = isInitializer;
        this.closure = closure;
        this.declaration = declaration;
    }

    /**
     * [Cap. 12] Cria um vínculo (binding) entre o método e uma instância.
     * Retorna uma nova função cujo closure tem uma variável "this" definida.
     */
    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        // O método vinculado mantém a propriedade de ser (ou não) um inicializador.
        return new LoxFunction(declaration, environment, isInitializer);
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // [Cap. 10] Cria um novo ambiente para a execução da função,
        // tendo o closure original como pai (escopo léxico).
        Environment environment = new Environment(closure);

        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme,
                arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            // [Cap. 12] Regra do Construtor:
            // Se estamos num inicializador, um 'return' (mesmo vazio) deve retornar 'this'.
            // O Resolver já garante que não podemos retornar um valor explicitamente.
            if (isInitializer) return closure.getAt(0, "this");

            return returnValue.value;
        }

        // [Cap. 12] Se a função terminar sem 'return' e for um init, retorna 'this' implicitamente.
        if (isInitializer) return closure.getAt(0, "this");

        return null;
    }
}