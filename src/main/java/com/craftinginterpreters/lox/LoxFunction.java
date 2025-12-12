package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;

  LoxFunction(Stmt.Function declaration) {
    this.declaration = declaration;
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
    // [Cap. 10] Cria um novo ambiente (escopo) para a execução da função.
    // Esse ambiente é filho do ambiente global (por enquanto, até chegarmos em Closures no Cap 11).
    Environment environment = new Environment(interpreter.globals);

    // Define os parâmetros no escopo da função
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme,
          arguments.get(i));
    }

    try {
      // Executa o corpo da função
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
      // [Cap. 10] Se houver um 'return', capturamos a exceção e devolvemos o valor
      return returnValue.value;
    }

    return null;
  }
}
