package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Ferramenta de Metaprogramação.
 * Este script não é executado pelo usuário final do interpretador.
 * Ele é usado pelos desenvolvedores para gerar automaticamente as classes boilerplate
 * da AST (Expr.java e Stmt.java), evitando erros manuais e digitação repetitiva.
 *
 * Referência: Crafting Interpreters - Capítulo 5 (Representing Code).
 */
public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];

        // [Cap. 5] Definição da Gramática de Expressões (Expr).
        // Atualizado para incluir estruturas dos Capítulos 8 (Variáveis) e 9 (Lógica).
        defineAst(outputDir, "Expr", Arrays.asList(
            "Assign   : Token name, Expr value",                 // [Cap. 8] Atribuição
            "Binary   : Expr left, Token operator, Expr right",  // [Cap. 5] Binário
            "Grouping : Expr expression",                        // [Cap. 5] Agrupamento
            "Literal  : Object value",                           // [Cap. 5] Literal
            "Logical  : Expr left, Token operator, Expr right",  // [Cap. 9] Lógico (and/or)
            "Unary    : Token operator, Expr right",             // [Cap. 5] Unário
            "Variable : Token name"                              // [Cap. 8] Variável
        ));

        // [Cap. 8] Definição da Gramática de Declarações (Stmt).
        // Isso não existia no script original do Cap. 5, mas é necessário para o Cap. 8+.
        defineAst(outputDir, "Stmt", Arrays.asList(
            "Block      : List<Stmt> statements",                        // [Cap. 8] Bloco
            "Expression : Expr expression",                              // [Cap. 8] Expr Statement
            "If         : Expr condition, Stmt thenBranch, Stmt elseBranch", // [Cap. 9] If
            "Print      : Expr expression",                              // [Cap. 8] Print
            "Var        : Token name, Expr initializer",                 // [Cap. 8] Var
            "While      : Expr condition, Stmt body"                     // [Cap. 9] While
        ));
    }

    /**
     * [Cap. 5] Método principal de geração de arquivo.
     * Cria o arquivo .java, define o pacote e a estrutura da classe base.
     */
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

        // Gera a interface Visitor (Padrão de Projeto)
        defineVisitor(writer, baseName, types);

        // Gera as classes estáticas aninhadas para cada tipo (Ex: Binary, Grouping...)
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        // O método abstrato accept() base
        writer.println();
        writer.println("  abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    /**
     * [Cap. 5] Gera a interface Visitor.
     * Essencial para separar a lógica de interpretação da estrutura de dados.
     */
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

    /**
     * [Cap. 5] Gera o corpo das classes (Campos, Construtor e Método Accept).
     */
    private static void defineType(
            PrintWriter writer, String baseName,
            String className, String fieldList) {
        writer.println("  static class " + className + " extends " + baseName + " {");

        // Construtor
        writer.println("    " + className + "(" + fieldList + ") {");

        // Inicialização dos campos
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("      this." + name + " = " + name + ";");
        }

        writer.println("    }");

        // Implementação do padrão Visitor
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
