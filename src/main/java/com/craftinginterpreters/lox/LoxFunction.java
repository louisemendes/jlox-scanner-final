package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  
  // [Cap. 10] Closure: O ambiente que estava ativo quando a função foi declarada.
  private final Environment closure;

  // [Cap. 12 - NOVO] Indica se esta função é um inicializador (construtor).
  private final boolean isInitializer;

  LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
    this.isInitializer = isInitializer;
    this.closure = closure;
    this.declaration = declaration;
  }

  /**
   * [Cap. 12] Cria um vínculo (binding) entre a função e uma instância.
   * Quando acessamos um método (ex: bolo.comer), precisamos criar uma nova versão dessa função
   * que tenha um ambiente específico onde a variável "this" existe e aponta para o "bolo".
   */
  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);
    // Repassamos 'isInitializer' porque o método vinculado continua sendo (ou não) um construtor.
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
    // [Cap. 10] ATUALIZADO: Usamos o 'closure' capturado como pai, e não mais o 'globals'.
    // Isso permite que a função acesse variáveis que estavam no escopo quando ela foi criada.
    Environment environment = new Environment(closure);

    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme,
          arguments.get(i));
    }

    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
      // [Cap. 12] Se for um inicializador, um retorno vazio devolve 'this'.
      // Construtores sempre retornam a instância que está sendo criada.
      if (isInitializer) return closure.getAt(0, "this");

      return returnValue.value;
    }

    // [Cap. 12] Se for inicializador, retorna 'this' ao final do bloco implicitamente.
    if (isInitializer) return closure.getAt(0, "this");

    return null;
  }
}