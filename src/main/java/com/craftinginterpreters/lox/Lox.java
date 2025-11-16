package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    // 4.1.1: Sinalizador de erro estático
    static boolean hadError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Uso: jlox [script]");
            System.exit(64); // EX_USAGE
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) System.exit(65); // EX_DATAERR (Erro no código)
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false; // Reseta o erro no REPL
        }
    }

    private static void run(String source) {
        // 4.4: Inicia o Scanner
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        
        // Apenas imprime os tokens (para teste do capítulo 4)
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    // 4.1.1: Funções de Tratamento de Erro
    public static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println(
            "[linha " + line + "] Erro" + where + ": " + message);
        hadError = true;
    }
}