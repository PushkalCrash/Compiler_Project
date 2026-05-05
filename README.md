

#  Compiler Project

A simple educational **compiler visualization project** that demonstrates the complete working of a compiler — from source code input to final target code generation.

This project helps students understand how compilers process code through different phases like lexical analysis, parsing, semantic checking, optimization, and code generation.

---

##  Features

* 🔹 Lexical Analysis (Tokenization)
* 🔹 Syntax Analysis (Parse Tree / AST)
* 🔹 Semantic Analysis (Error detection + Symbol Table)
* 🔹 Intermediate Code Generation (3-address code)
* 🔹 Code Optimization
* 🔹 Target Code Generation (Assembly-like output)
* 🔹 Visual representation of compiler phases (GUI)

---

## Project Structure

```
src/compiler/
│── Token.java               # Token definitions
│── Lexer.java               # Phase 1: Lexical Analysis
│── ASTNode.java             # AST structure
│── Parser.java              # Phase 2: Syntax Analysis
│── SemanticAnalyzer.java    # Phase 3: Semantic Analysis
│── IntermediateCodeGen.java # Phase 4: Intermediate Code
│── CodeOptimizer.java       # Phase 5: Optimization
│── TargetCodeGen.java       # Phase 6: Target Code
│── TreePanel.java           # AST Visualization
│── CompilerUI.java          # GUI Interface
│── Main.java                # Entry Point
```

---

##  How to Run

###  Step 1: Compile the project

```bash
javac -d out src/compiler/*.java
```

### Step 2: Run the application

```bash
java -cp out compiler.Main
```

---

##  How It Works

1. Enter C-like code in the editor
2. Click **Compile**
3. The system processes your code through 6 phases:

| Phase                 | Description                   |
| --------------------- | ----------------------------- |
| **Lexical Analysis**  | Converts code into tokens     |
| **Syntax Analysis**   | Builds parse tree (AST)       |
| **Semantic Analysis** | Checks errors & types         |
| **Intermediate Code** | Generates 3-address code      |
| **Optimization**      | Improves code efficiency      |
| **Target Code**       | Produces assembly-like output |

If an error occurs, compilation stops at that phase.

---

## Example

Input:

```c
int a = 5 + 3;
```

Intermediate Output:

```
t1 = 5 + 3
a = t1
```

---

## Purpose

This project is built to:

* Help students understand compiler design concepts
* Visualize each phase step-by-step
* Serve as a learning tool for courses like **Compiler Design / COA**
* Demonstrate how real compilers work internally

---

##  Tech Stack

* **Language:** Java
* **UI:** Java Swing
* **Concepts:** Compiler Design, Data Structures, Parsing

---

##  Requirements

* Java JDK 8 or above
* Basic knowledge of compiler concepts

---

