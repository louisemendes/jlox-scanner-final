package com.craftinginterpreters.lox;

import java.util.List;
import java.util.ArrayList; // [Cap. 10] chamada/argumentos
import java.util.Map;       // [Cap. 11] mapa de resolução (locals)
import java.util.HashMap;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Interpreter (Interpretador)
 *
 * Executa a Árvore de Sintaxe Abstrata (AST) produzida pelo Parser.
 * Implementa os visitantes Expr.Visitor e Stmt.Visitor e mantém o estado
 * de execução através de Environment(s).
 *
 * Referências:
 * - Crafting Interpreters — Cap. 7 (Evaluating Expressions)
 * - Crafting Interpreters — Cap. 8 (Statements and State)
 * - Crafting Interpreters — Cap. 9 (Control Flow)
 * - Crafting Interpreters — Cap. 10 (Functions)
 * - Crafting Interpreters — Cap. 11 (Resolving and Binding)
 * - Crafting Interpreters — Cap. 12 (Classes)
 */
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    /**
     * Ambiente global (contém funções nativas e variáveis globais).
     * Imutável enquanto instância do Interpretador.
     * Referência: CI — Cap. 10 (Native functions / globals).
     */
    private final Environment globals = new Environment();

    /**
     * Ambiente atual (stack de escopos). Inicialmente aponta para 'globals'.
     * Referência: CI — Cap. 8 (Environments).
     */
    private Environment environment = globals;

    /**
     * Mapa usado pelo Resolver para indicar a distância (número de escopos)
     * entre o escopo atual e o escopo onde uma variável foi declarada.
     *
     * Referência: CI — Cap. 11 (Resolver & Binding)
     */
    private final Map<Expr, Integer> locals = new HashMap<>();

    /**
     * Construtor: registra funções nativas no ambiente global.
     * Ex.: clock()
     *
     * Referência: CI — Cap. 10 (Defining native functions in globals)
     */
    public Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    /**
     * Executa uma lista de declarações (programa).
     * Lança runtime errors para serem tratados externamente.
     *
     * Referência: CI — Cap. 8 (Executing statements)
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

    /**
     * Chamado pelo Resolver para gravar a distância de resolução de uma expressão.
     * (Expressões que referenciam variáveis recebem uma distância para lookup rápido).
     *
     * Referência: CI — Cap. 11 (Resolver)
     */
    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    // ---------------------------------------------------------------------
    // Execution helpers
    // ---------------------------------------------------------------------

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    /**
     * Executa um bloco em um ambiente fornecido, preservando o ambiente anterior.
     *
     * Referência: CI — Cap. 8 (Blocks / Environments)
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

    // ---------------------------------------------------------------------
    // Stmt.Visitor implementations
    // ---------------------------------------------------------------------

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    /**
     * Execução de declaração de classe.
     *
     * Referência: CI — Cap. 12 (Classes)
     * Gramática: classDecl → "class" IDENTIFIER "{" function* "}"
     */
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        // Define o nome da classe no escopo atual antes de construir métodos (permite referências recursivas)
        environment.define(stmt.name.lexeme, null);

        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            boolean isInitializer = method.name.lexeme.equals("init");
            LoxFunction function = new LoxFunction(method, environment, isInitializer);
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
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    /**
     * Declaração de função: armazenamos um LoxFunction no ambiente atual.
     *
     * Referência: CI — Cap. 10 (Function declarations)
     */
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    /**
     * Return statement — interrompe o fluxo atual lançando a exceção Return.
     *
     * Referência: CI — Cap. 10 (Return)
     */
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);
        throw new Return(value); // Exceção top-level usada para controlar retorno de função.
    }

    // ---------------------------------------------------------------------
    // Expr.Visitor implementations
    // ---------------------------------------------------------------------

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    /**
     * Atribuição: usa 'locals' para decidir onde atribuir (assignAt) ou tratar como global.
     *
     * Referência: CI — Cap. 11 (Resolving) e Cap. 8 (Assignment semantics)
     */
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

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
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
        }

        // Unreachable
        return null;
    }

    /**
     * Chamada (call) a função/objeto.
     *
     * Referência: CI — Cap. 10 (Call and Arity)
     */
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

        LoxCallable function = (LoxCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " + arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    /**
     * Acesso a propriedade (get).
     *
     * Referência: CI — Cap. 12 (Property access)
     */
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

    /**
     * Expressões lógicas com short-circuit (and / or).
     *
     * Referência: CI — Cap. 9 (Short-circuit logic)
     */
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    /**
     * Definição de propriedade (set).
     *
     * Referência: CI — Cap. 12 (Set on instances)
     */
    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);
        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((LoxInstance) object).set(expr.name, value);
        return value;
    }

    /**
     * 'this' — busca usando o mecanismo de resolução (locals).
     *
     * Referência: CI — Cap. 12 (this binding)
     */
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
                return -(double) right;
        }

        // Unreachable
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    /**
     * Busca variável usando a informação de resolução (locals).
     * Se não houver informação (não resolvida), assume global.
     *
     * Referência: CI — Cap. 11 (Resolver & Binding)
     */
    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    // ---------------------------------------------------------------------
    // Helpers: Type checks, truthiness, equality e stringify
    // ---------------------------------------------------------------------

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
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    /**
     * Converte valores para representação textual utilizada por 'print'.
     * Remove ".0" de doubles inteiros (comportamento do livro).
     */
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
