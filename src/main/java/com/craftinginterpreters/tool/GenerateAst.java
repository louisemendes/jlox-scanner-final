package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * GenerateAst — Ferramenta de Metaprogramação
 *
 * Esta ferramenta não é utilizada pelo usuário final do interpretador.
 * Seu propósito é gerar automaticamente as classes da Árvore de Sintaxe Abstrata
 * (Expr.java e Stmt.java), eliminando repetição, evitando erros manuais e
 * garantindo consistência entre nós de expressão e declaração.
 *
 * Referência principal:
 *   Crafting Interpreters — Capítulo 5 (Representing Code)
 *
 * Extensões implementadas:
 *  - Inclusão dos tipos dos Capítulos 8, 9, 10 e 12:
 *      * Declarações (Stmt.*)
 *      * Funções e chamadas
 *      * Atribuição
 *      * Lógica
 *      * Classes, propriedades e "this"
 *
 * A definição das gramáticas (Lista de tipos) é traduzida automaticamente
 * para classes Java concretas com construtores, campos e método accept().
 */
public class GenerateAst {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        String outputDir = args[0];

        // ---------------------------------------------------------------------
        // Definição da Gramática de Expressões (Expr)
        // Cap. 5 + extensões dos Caps. 8–12
        // ---------------------------------------------------------------------
        defineAst(outputDir, "Expr", Arrays.asList(
            "Assign   : Token name, Expr value",                         // Cap. 8 – Assignment
            "Binary   : Expr left, Token operator, Expr right",          // Cap. 5 – Binary Expression
            "Call     : Expr callee, Token paren, List<Expr> arguments", // Cap. 10 – Function Call
            "Get      : Expr object, Token name",                        // Cap. 12 – Property Access
            "Grouping : Expr expression",                                // Cap. 5 – Grouping
            "Literal  : Object value",                                   // Cap. 5 – Literal
            "Logical  : Expr left, Token operator, Expr right",          // Cap. 9 – Logical Operators
            "Set      : Expr object, Token name, Expr value",            // Cap. 12 – Property Assignment
            "This     : Token keyword",                                  // Cap. 12 – this
            "Unary    : Token operator, Expr right",                     // Cap. 5 – Unary Expression
            "Variable : Token name"                                      // Cap. 8 – Variable Expression
        ));

        // ---------------------------------------------------------------------
        // Definição da Gramática de Declarações (Stmt)
        // Cap. 8 e extensões dos Caps. 9–12
        // ---------------------------------------------------------------------
        defineAst(outputDir, "Stmt", Arrays.asList(
            "Block      : List<Stmt> statements",                            // Cap. 8 – Blocks
            "Class      : Token name, List<Stmt.Function> methods",          // Cap. 12 – Class Declaration
            "Expression : Expr expression",                                  // Cap. 8 – Expression Statement
            "Function   : Token name, List<Token> params, List<Stmt> body",  // Cap. 10 – Function Declaration
            "If         : Expr condition, Stmt thenBranch, Stmt elseBranch", // Cap. 9 – If Statement
            "Print      : Expr expression",                                  // Cap. 8 – Print Statement
            "Return     : Token keyword, Expr value",                        // Cap. 10 – Return
            "Var        : Token name, Expr initializer",                     // Cap. 8 – Variable Declaration
            "While      : Expr condition, Stmt body"                         // Cap. 9 – While Loop
        ));
    }

    // -------------------------------------------------------------------------
    // Método principal de geração de arquivo
    // Produz o arquivo <baseName>.java contendo:
    //   - Interface Visitor
    //   - Classe abstrata base
    //   - Classes concretas para cada tipo de nó
    // -------------------------------------------------------------------------
    private static void defineAst(
            String outputDir, String baseName, List<String> types)
            throws IOException {

        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        // Interface Visitor (Padrão Visitor — Cap. 5)
        defineVisitor(writer, baseName, types);

        // Geração das classes concretas
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        writer.println();
        writer.println("  abstract <R> R accept(Visitor<R> visitor);");
        writer.println("}");
        writer.close();
    }

    // -------------------------------------------------------------------------
    // Gera interface Visitor<R>
    // Cada método "visit" corresponde a um tipo da AST
    // -------------------------------------------------------------------------
    private static void defineVisitor(
            PrintWriter writer, String baseName, List<String> types) {

        writer.println("  interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("    R visit" + typeName + baseName + "(" +
                    typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("  }");
    }

    // -------------------------------------------------------------------------
    // Gera cada classe concreta que representa um nó da AST
    // Inclui:
    //  - Construtor
    //  - Campos imutáveis
    //  - Implementação do método accept()
    // -------------------------------------------------------------------------
    private static void defineType(
            PrintWriter writer, String baseName,
            String className, String fieldList) {

        writer.println("  static class " + className + " extends " + baseName + " {");

        // Construtor
        writer.println("    " + className + "(" + fieldList + ") {");

        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("      this." + name + " = " + name + ";");
        }

        writer.println("    }");

        // Implementação do Visitor
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit" +
                className + baseName + "(this);");
        writer.println("    }");

        // Declaração dos campos
        writer.println();
        for (String field : fields) {
            writer.println("    final " + field + ";");
        }

        writer.println("  }");
    }
}
