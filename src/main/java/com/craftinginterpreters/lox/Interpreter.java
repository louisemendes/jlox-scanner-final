package com.craftinginterpreters.lox;

import java.util.List;
import java.util.ArrayList; // [Cap. 10] Necessário para lista de argumentos
import java.util.Map;       // [Cap. 11] Necessário para o mapa de resolução

import com.craftinginterpreters.lox.Stmt.Return;

import java.util.HashMap;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Interpreter (Interpretador).
 * Responsável por executar a Árvore de Sintaxe Abstrata (AST) gerada pelo Parser.
 * Implementa o padrão Visitor para percorrer os nós da árvore e calcular os resultados.
 *
 * Referências:
 * - Cap. 7 (Evaluating Expressions)
 * - Cap. 8 (Statements and State)
 * - Cap. 9 (Control Flow)
 * - Cap. 10 (Functions)
 * - Cap. 11 (Resolving and Binding)
 * - Cap. 12 (Classes)
 */
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    // [Cap. 10] Ambiente Global.
    // Mantemos uma referência fixa para definir funções nativas depois.
    final Environment globals = new Environment();

    // [Cap. 8] Ambiente Global/Atual.
    // Começa apontando para o global.
    private Environment environment = globals;

    // [Cap. 11] Mapa que guarda a "distância" (número de escopos) até a variável.
    private final Map<Expr, Integer> locals = new HashMap<>();

    /**
     * [Cap. 10] Construtor do Interpretador.
     * Define as funções nativas globais.
     */
    public Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    /**
     * [Cap. 8] Ponto de entrada para execução de uma lista de comandos.
     */
    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    // [Cap. 11] Método chamado pelo Resolver para informar a distância de uma variável.
    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    // --- Execução de Statements (Comandos) ---

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    /**
     * [Cap. 8] Executa um bloco de código dentro de um escopo específico.
     * Salva o ambiente anterior, troca para o novo, executa, e restaura o anterior.
     */
    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    // [Cap. 12] Execução da Declaração de Classe
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);

        // Transforma os métodos da sintaxe (Stmt) para runtime (LoxFunction)
        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            // O ambiente "closure" aqui é o ambiente onde a CLASSE foi definida
            LoxFunction function = new LoxFunction(method, environment);
            methods.put(method.name.lexeme, function);
        }

        LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        // Se houver inicializador (var a = 1;), avalia.
        // Se não (var a;), o valor permanece null (nil).
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        // [Cap. 9] Executa o corpo repetidamente enquanto a condição for verdadeira.
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    // [Cap. 10] Declaração de função.
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        // Agora passamos o ambiente atual (environment) para ser o closure da função
        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    // [Cap. 10] Retorno de função.
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }

        // Forçamos o uso da classe de Exceção explicitamente
        throw new com.craftinginterpreters.lox.Return(value);
    }

    // --- Avaliação de Expressões ---

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        // [Cap. 11] Verifica se temos informação de distância para essa variável
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }

        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                // [Cap. 7] Sobrecarga do operador + (Soma numérica ou Concatenação).
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator,
                    "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
        }

        // Unreachable.
        return null;
    }

    // [Cap. 10] Lógica de execução de chamadas
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable)callee;

        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                function.arity() + " arguments but got " +
                arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    // [Cap. 12] Acesso a propriedade: objeto.propriedade
    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        
        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(expr.name);
        }

        throw new RuntimeError(expr.name, "Only instances have properties.");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        // [Cap. 9] Lógica de Curto-Circuito (Short-circuit).
        Object left = evaluate(expr.left);

        if (expr.operator.type == OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    // [Cap. 12] Definição de propriedade: objeto.propriedade = valor
    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);
        return value;
    }

    // [Cap. 12] Execução do 'this'
    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        // [Cap. 11] Busca variável usando a distância resolvida ou global
        return lookUpVariable(expr.name, expr);
    }

    // [Cap. 11] Auxiliar para buscar variável na distância correta
    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    // --- Utilitários (Helpers) ---

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }
}