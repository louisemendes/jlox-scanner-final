package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Resolver — Analisador de Escopo Estático
 *
 * Responsável por realizar a análise semântica estática antes da execução,
 * determinando exatamente a quais declarações cada ocorrência de variável
 * se refere. Esse processo resolve ambiguidades de escopo dinâmico
 * e permite a implementação de closures e ligação lexical (lexical scoping).
 *
 * Referências:
 *  - Crafting Interpreters — Cap. 11 (Resolving and Binding)
 *  - Crafting Interpreters — Cap. 12 (Classes)
 *
 * Funcionamento geral:
 *  1. Percorre a AST antes da interpretação.
 *  2. Mantém uma pilha de escopos (stack) composta de mapas { nome → inicializado? }.
 *  3. Define a “distância” até a declaração de cada variável.
 *  4. Informa essas distâncias ao Interpreter via interpreter.resolve(expr, depth).
 *
 * Sem o Resolver, lookup de variáveis dependeria apenas do ambiente dinâmico —
 * impedindo closures corretos e levando a ambiguidades de escopo.
 */
class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final Interpreter interpreter;

    /** Pilha de escopos léxicos: cada nível é um mapa { nome: inicializado? }. */
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    /** Rastreia o contexto funcional atual (usado para validar retornos). */
    private FunctionType currentFunction = FunctionType.NONE;

    /** Rastreia o contexto de classe atual (usado para validar uso de 'this'). */
    private ClassType currentClass = ClassType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    // -------------------------------------------------------------------------
    // Enumerações internas — estados de contexto
    // -------------------------------------------------------------------------

    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,  // Cap. 12 — função "init"
        METHOD        // Cap. 12 — métodos de instância
    }

    private enum ClassType {
        NONE,
        CLASS
    }

    // -------------------------------------------------------------------------
    // Ponto de entrada
    // -------------------------------------------------------------------------

    /**
     * Resolve uma lista de declarações (arquivo completo).
     */
    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    // -------------------------------------------------------------------------
    // Visitantes de STATEMENTS
    // -------------------------------------------------------------------------

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    /**
     * Resolução de declaração de classe.
     *
     * Cap. 12:
     *  - Declara o nome no escopo externo.
     *  - Abre um novo escopo contendo "this".
     *  - Resolve cada método individualmente.
     */
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);

        // Escopo onde 'this' está disponível
        beginScope();
        scopes.peek().put("this", true);

        for (Stmt.Function method : stmt.methods) {
            FunctionType type = method.name.lexeme.equals("init")
                    ? FunctionType.INITIALIZER
                    : FunctionType.METHOD;

            resolveFunction(method, type);
        }

        endScope();
        currentClass = enclosingClass;
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return a value from an initializer.");
            }
            resolve(stmt.value);
        }

        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    // -------------------------------------------------------------------------
    // Visitantes de EXPRESSIONS
    // -------------------------------------------------------------------------

    /**
     * Variável sendo lida.
     * Cap. 11: é erro ler variável não inicializada dentro do mesmo escopo.
     */
    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty()
                && Boolean.FALSE.equals(scopes.peek().get(expr.name.lexeme))) {
            Lox.error(expr.name, "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr arg : expr.arguments) resolve(arg);
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null; // Literais não têm escopo.
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
            return null;
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    // -------------------------------------------------------------------------
    // Helpers internos
    // -------------------------------------------------------------------------

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    /**
     * Resolve corpo de uma função/método.
     *
     * Cap. 11:
     *  - Abre novo escopo para parâmetros.
     *  - Parâmetros são declarados e definidos imediatamente.
     *  - Corpo é resolvido em seguida.
     */
    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosing = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();

        currentFunction = enclosing;
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    /**
     * Declara um nome no escopo atual sem defini-lo.
     * Cap. 11: permite detectar leitura prematura.
     */
    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Already a variable with this name in this scope.");
        }

        scope.put(name.lexeme, false); // declarado, não inicializado
    }

    /**
     * Marca variável como definida (inicialização completa).
     */
    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    /**
     * Determina a que distância (quantos escopos acima) está uma variável.
     * Essa informação é enviada ao Interpreter.
     */
    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
        // Não encontrado: variável global. Nada é registrado.
    }
}
