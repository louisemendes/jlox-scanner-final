Construção do Interpretador Lox baseado em Crafting Interpreters (Cap. 4–12)
<div align="center">
Projeto Final – Compiladores / Construção de Linguagens
Implementação completa de Scanner • Parser • Resolver • Interpreter • Funções • Classes
</div>
1. Integrantes da Equipe
<table> <tr> <th>Nome Completo</th> <th>Usuário GitHub</th> </tr> <tr> <td><strong>Raianny Cristina Ferreira da Silva</strong></td> <td><a href="https://github.com/raianny-cristina">raianny-cristina</a></td> </tr> <tr> <td><strong>Louise Reis Mendes</strong></td> <td><a href="https://github.com/louisemendes">louisemendes</a></td> </tr> </table>
2. Resumo do Projeto

Este repositório contém a implementação completa do interpretador JLox, seguindo fielmente o desenvolvimento do livro Crafting Interpreters, de Robert Nystrom.

O objetivo central é compreender e implementar todas as fases essenciais de um interpretador real, culminando em uma linguagem totalmente funcional com:

Funções e closures

Resolução estática de escopo

Classes, instâncias e métodos

Inicializadores (init)

Execução REPL e execução via arquivos

3. Arquitetura do Interpretador

A seguir apresentamos a arquitetura geral do sistema, organizada em camadas, com as responsabilidades de cada módulo:

<div align="center"> <table> <tr> <th>Camada</th> <th>Arquivo(s)</th> <th>Responsabilidade</th> </tr> <tr> <td><strong>Entrada / Interface</strong></td> <td> Lox.java </td> <td>Executa REPL e lê arquivos .lox, controla erros de alto nível.</td> </tr> <tr> <td><strong>Análise Léxica</strong></td> <td>Scanner.java</td> <td>Tokeniza o código, gerando objetos <code>Token</code>.</td> </tr> <tr> <td><strong>Análise Sintática</strong></td> <td>Parser.java</td> <td>Constrói a AST a partir da sequência de tokens.</td> </tr> <tr> <td><strong>AST (Estruturas)</strong></td> <td>Expr.java, Stmt.java (gerados por <code>GenerateAst</code>)</td> <td>Representação abstrata das instruções e expressões.</td> </tr> <tr> <td><strong>Resolução de Escopo</strong></td> <td>Resolver.java</td> <td>Realiza análise semântica estática (binding léxico).</td> </tr> <tr> <td><strong>Ambientes / Escopos</strong></td> <td>Environment.java</td> <td>Armazena variáveis, estados e closures.</td> </tr> <tr> <td><strong>Execução</strong></td> <td>Interpreter.java</td> <td>Percorre a AST e executa instruções em tempo de execução.</td> </tr> <tr> <td><strong>POO</strong></td> <td>LoxClass.java, LoxInstance.java, LoxFunction.java</td> <td>Implementa classes, instâncias, métodos e binding de <code>this</code>.</td> </tr> <tr> <td><strong>Metaprogramação</strong></td> <td>GenerateAst.java</td> <td>Gera automaticamente as classes da AST.</td> </tr> </table> </div>
4. Progresso por Capítulo (Crafting Interpreters)
<div align="center">
Resumo das Etapas Implementadas
</div> <table> <tr> <th>Capítulo</th> <th>Conteúdo</th> <th>Status</th> </tr> <tr> <td><strong>Cap. 4</strong></td> <td>Lox Core: REPL, arquivos, erros.</td> <td>✅ Completo</td> </tr> <tr> <td><strong>Cap. 5</strong></td> <td>Scanner (Tokens, literais, lexemas, palavras reservadas).</td> <td>✅ Completo</td> </tr> <tr> <td><strong>Cap. 6–7</strong></td> <td>Parser (Expressões, precedência, erros).</td> <td>✅ Completo</td> </tr> <tr> <td><strong>Cap. 8</strong></td> <td>Declarações, variáveis, blocos de escopo.</td> <td>✅ Completo</td> </tr> <tr> <td><strong>Cap. 9</strong></td> <td>Controle de fluxo: if, else, while, lógico.</td> <td>✅ Completo</td> </tr> <tr> <td><strong>Cap. 10</strong></td> <td>Declaração e chamada de funções, return, closures.</td> <td>✅ Completo</td> </tr> <tr> <td><strong>Cap. 11</strong></td> <td>Resolver: análise estática de variáveis e escopos.</td> <td>✅ Completo</td> </tr> <tr> <td><strong>Cap. 12</strong></td> <td>Classes, métodos, <code>this</code>, inicializadores e instâncias.</td> <td>✅ Completo</td> </tr> </table>
5. Descrição Técnica das Funcionalidades
5.1 Scanner (Análise Léxica)

Implementa reconhecimento de:

literais numéricos

strings

identificadores

operadores simples e compostos

palavras-chave

Mapeamento O(1) para palavras reservadas

5.2 Parser (Análise Sintática)

Utiliza uma gramática LL recursiva direta para construir árvore de precedência.

Suporta:

unary → ( "!" | "-" ) unary | call
binary → unary ( ( "==" | "!=" | "<" | ... ) unary )*
grouping → "(" expression ")"
call → primary ( "(" arguments? ")" )*

5.3 Resolver (Escopo Léxico Estático)

Detecta variáveis usadas antes da definição

Determina profundidade do escopo

Rejeita return inválido

Rejeita uso de this fora de classes

Permite closures reais

Esta etapa garante a semântica correta antes da execução.

5.4 Interpreter (Execução)

Usa o padrão Visitor

Executa nós da AST

Controle de fluxo

Avaliação de expressões

Operações matemáticas, lógicas e comparações

Execução de funções

Execução de classes e métodos

5.5 POO Completa (Cap. 12)

Com as classes:

LoxClass

LoxInstance

LoxFunction

O interpretador suporta:

declaração de classes

instâncias

métodos com binding automático

inicializador init() como construtor

acesso e modificação de atributos em objetos

sintaxe:

class Pessoa {
  init(nome) {
    this.nome = nome;
  }

  falar() {
    print this.nome + " está falando.";
  }
}

6. Como Executar o Interpretador
6.1 Pré-requisitos

JDK 8+

Maven instalado (opcional)

6.2 Compilar
mvn clean compile

6.3 Executar no modo REPL
java -cp target/classes com.craftinginterpreters.lox.Lox

6.4 Executar um arquivo
java -cp target/classes com.craftinginterpreters.lox.Lox programa.lox

7. Demonstração Obrigatória (para o vídeo)
7.1 No REPL, demonstrar:

variáveis

operadores

funções

closures

classes

métodos e uso do this

7.2 Programa Livre (exemplo)

Fibonacci

Funções recursivas

Jogos simples

7.3 Programa com Classes (obrigatório)

classe com init

instâncias

métodos

atributos dinâmicos

8. Organização do Repositório
/src/com/craftinginterpreters/lox/
│── Lox.java
│── Token.java
│── TokenType.java
│── Scanner.java
│── Parser.java
│── Expr.java
│── Stmt.java
│── Environment.java
│── Interpreter.java
│── Resolver.java
│── LoxCallable.java
│── LoxFunction.java
│── LoxClass.java
│── LoxInstance.java
│── Return.java
│── RuntimeError.java
└── ...

/src/com/craftinginterpreters/tool/
│── GenerateAst.java

9. Conclusão

Este projeto implementa um interpretador completo, apresentando:

suporte a funções e closures

resolução estática e escopo léxico

classes, objetos e métodos

REPL interativo

execução de arquivos .lox

arquitetura modular e extensível

O resultado final atende plenamente os requisitos dos capítulos 4–12 do Crafting Interpreters, concluindo um interpretador funcional e didaticamente estruturado.

10. Referência

Robert Nystrom – Crafting Interpreters.
https://craftinginterpreters.com/
