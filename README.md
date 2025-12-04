# Lox: Análise Léxica

Projeto de construção do interpretador Lox, baseado no livro Crafting Interpreters.

### Integrantes da Dupla
| Nome Completo | Usuário GitHub |
| :--- | :--- |
| **RAIANNY CRISTINA FERREIRA DA SILVA** | raianny-cristina |
| **LOUISE REIS MENDES** | louisemendes |

---

## 🧠 Propósito e Estrutura do Projeto

O objetivo principal deste projeto é construir um interpretador para entender o funcionamento interno de linguagens de programação.

A arquitetura do projeto é dividida em módulos que replicam as fases de um compilador/interpretador:

| Módulo | Fase do Interpretador | Função |
| :--- | :--- | :--- |
| **Scanner** | Análise Léxica (Tokenização) | Lê o código e transforma caracteres em *Tokens* (unidades significativas). |
| **Parser** | Análise Sintática | Processa os *Tokens* e constrói a **Árvore Sintática Abstrata (AST)**. |
| **Resolver (Futuro)** | Análise Semântica (Estática) | Analisará a AST para resolver nomes de variáveis (Escopo e *Binding*). |
| **Interpreter** | Execução (Runtime) | Percorre a AST e executa o código Lox, avaliando expressões e instruções. |
| **Environment** | Estado e Escopo | Gerencia o *Estado* do interpretador, armazenando e resolvendo variáveis. |

---

## 🛠️ Status Atual e Funcionalidades Implementadas

O projeto progrediu pelas seguintes etapas do livro "Crafting Interpreters" (equivalentes às atividades da disciplina):

| Etapa/Capítulo | Funcionalidade | Implementação | Status |
| :--- | :--- | :--- | :--- |
| **Cap. 4 (Lox Core)** | Estrutura Básica | Ponto de entrada (`Lox.java`), tratamento de erros e modo REPL. | ✅ Completo |
| **Cap. 5 (Scanning)** | Análise Léxica | Definição de `Token` e `TokenType`, reconhecimento de literais, identificadores e palavras-chave. | ✅ Completo |
| **Cap. 6 & 7 (Parsing)** | Parsing de Expressões | Implementação do *Parser* para precedência de operadores (Unários, Binários), agrupamento e tratamento de erros de sintaxe. | ✅ Completo |
| **Cap. 8 (Statements & State)** | Instruções e Variáveis | Execução de instruções (`Stmt`), declaração (`var`), atribuição (`=`), `print` e a classe `Environment`. | ✅ Concluído |
| **Cap. 9 (Control Flow)** | Fluxo de Controle | Implementação de Blocos de código (`{}`) e condicionais (`if/else`) para gerenciamento de escopo. | ✅ Concluído |
| **Cap. 10 (While/For)** | Loops | Estruturas de repetição (`while` e `for`). | ⏳ Próxima Etapa |

---

## ▶️ Como Executar o JLOX

O interpretador é executado via linha de comando (CLI), após a compilação do projeto Java.

### Pré-requisitos
* Java Development Kit (JDK) 8 ou superior.
* Maven (recomendado, facilita a compilação).

O projeto está estruturado para ser executado como um projeto Maven ou diretamente via linha de comando.

### Comandos de Execução

O ponto de entrada é a classe `com.craftinginterpreters.lox.Lox`.

```bash
# 1. Compilação (usando Maven)
mvn clean compile

# 2. Execução: Modo REPL (Interativo)
# O interpretador espera que você digite comandos Lox.
java -cp target/classes com.craftinginterpreters.lox.Lox

# 3. Execução: Executando um Script
# O interpretador lê e executa um arquivo com extensão .lox
java -cp target/classes com.craftinginterpreters.lox.Lox path/para/seu/script.lox
