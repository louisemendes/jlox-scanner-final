package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  
  // [Cap. 10] Closure: O ambiente que estava ativo quando a função foi declarada.
  private final Environment closure;

  LoxFunction(Stmt.Function declaration, Environment closure) {
    this.closure = closure;
    this.declaration = declaration;
  }

  /**
   * [Cap. 12 - NOVO] Cria um vínculo (binding) entre a função e uma instância.
   * Quando acessamos um método (ex: bolo.comer), precisamos criar uma nova versão dessa função
   * que tenha um ambiente específico onde a variável "this" existe e aponta para o "bolo".
   */
  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);
    return new LoxFunction(declaration, environment);
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
      return returnValue.value;
    }

    return null;
  }
}