package compiler;

/**
 * Quick test harness: runs all 21 PDF programs through the compiler pipeline
 * and reports pass/fail for each phase.
 */
public class TestPrograms {

    private static final String[] PROGRAMS = {
        // Program 1 - Rectangle pattern
        "#include <stdio.h>\nint main() {\n    int i, j, n = 5;\n    for(i = 1; i <= n; i++) {\n        for(j = 1; j <= n; j++) {\n            if(i == 1 || i == n || j == 1 || j == n)\n                printf(\"* \");\n            else\n                printf(\"  \");\n        }\n        printf(\"\\n\");\n    }\n    return 0;\n}",

        // Program 2 - Multiplication table
        "#include <stdio.h>\nint main() {\n    int i, j;\n    for(i = 1; i <= 5; i++) {\n        for(j = 1; j <= 5; j++) {\n            printf(\"%d \", i * j);\n        }\n        printf(\"\\n\");\n    }\n    return 0;\n}",

        // Program 3 - Number triangle
        "#include <stdio.h>\nint main() {\n    int i, j;\n    for(i = 1; i <= 5; i++) {\n        for(j = 1; j <= i; j++) {\n            printf(\"%d \", j);\n        }\n        printf(\"\\n\");\n    }\n    printf(\"Pattern complete\\n\");\n    return 0;\n}",

        // Program 4 - Checkerboard (deliberate error: i,j not declared)
        "#include <stdio.h>\nint main()\n{\nfor(i=1;i<=8;i++){\n        for(j=1;j<=8;j++){\n            if((i+j)%2==0)\nprintf(\"W \");\n            else printf(\"B \");\n        }\n        printf(\"\\n\");\n    }\nreturn 0;\n}",

        // Program 5 - Star triangle
        "#include <stdio.h>\nint main()\n{\n    int i, j;\n    for(i = 1; i <= 5; i++)\n    {\n        for(j = 1; j <= i; j++)\n        {\n            printf(\"* \");\n        }\n        printf(\"\\n\");\n    }\n    return 0;\n}",

        // Program 8 - Palindrome
        "#include <stdio.h>\nint main() {\n    int num = 121, temp, rev = 0, digit;\n    temp = num;\n    while(num != 0) {\n        digit = num % 10;\n        rev = rev * 10 + digit;\n        num = num / 10;\n    }\n    if(temp == rev)\n        printf(\"Palindrome\\n\");\n    else\n        printf(\"Not Palindrome\\n\");\n    return 0;\n}",

        // Program 9 - While loop
        "#include <stdio.h>\nint main() {\n    int i = 1;\n    while(i <= 5) {\n        printf(\"Value: %d\\n\", i);\n        i++;\n    }\n    printf(\"Loop ended\\n\");\n    return 0;\n}",

        // Program 10 - Sum of digits
        "#include <stdio.h>\nint main() {\n    int num = 1234, sum = 0, digit;\n    while(num != 0) {\n        digit = num % 10;\n        sum = sum + digit;\n        num = num / 10;\n    }\n    printf(\"Sum of digits = %d\\n\", sum);\n    return 0;\n}",

        // Program 11 - Reverse number (uses /=)
        "#include <stdio.h>\nint main()\n{\nint num = 1234, rev = 0, rem;\nwhile(num != 0) {\nrem = num % 10;\nrev = rev * 10 + rem;\nnum /= 10;\n}\nprintf(\"Reverse = %d\", rev);\nreturn 0;\n}",

        // Program 14 - Largest of three
        "#include <stdio.h>\nint main() {\n    int a = 10, b = 25, c = 15;\n    if(a > b && a > c) {\n        printf(\"Largest is a = %d\\n\", a);\n    } else if(b > c) {\n        printf(\"Largest is b = %d\\n\", b);\n    } else {\n        printf(\"Largest is c = %d\\n\", c);\n    }\n    return 0;\n}",

        // Program 15 - Grade system
        "#include <stdio.h>\nint main()\n{\n    int marks = 75;\n    if(marks >= 90)\n        printf(\"Grade A\");\n    else if(marks >= 75)\n        printf(\"Grade B\");\n    else if(marks >= 50)\n        printf(\"Grade C\");\n    else\n        printf(\"Fail\");\n    return 0;\n}",

        // Program 17 - Leap year
        "#include <stdio.h>\nint main() {\n    int year = 2024;\n    if(year % 4 == 0) {\n        if(year % 100 == 0) {\n            if(year % 400 == 0)\n                printf(\"Leap Year\\n\");\n            else\n                printf(\"Not Leap Year\\n\");\n        } else {\n            printf(\"Leap Year\\n\");\n        }\n    } else {\n        printf(\"Not Leap Year\\n\");\n    }\n    return 0;\n}",

        // Program 18 - Positive/Negative Even/Odd
        "#include <stdio.h>\nint main() {\n    int num = -4;\n    if(num > 0) {\n        if(num % 2 == 0)\n            printf(\"Positive Even\\n\");\n        else\n            printf(\"Positive Odd\\n\");\n    } else if(num < 0) {\n        if(num % 2 == 0)\n            printf(\"Negative Even\\n\");\n        else\n            printf(\"Negative Odd\\n\");\n    } else {\n        printf(\"Number is Zero\\n\");\n    }\n    return 0;\n}",

        // Program 19 - Both positive check
        "#include <stdio.h>\nint main() {\n    int a = 10, b = 20;\n    if(a > 0) {\n        if(b > 0) {\n            printf(\"Both numbers are positive\\n\");\n        } else {\n            printf(\"a is positive, b is not\\n\");\n        }\n    } else {\n        printf(\"a is not positive\\n\");\n    }\n    return 0;\n}",

        // Program 21 - Login check (deliberate error: undeclared vars)
        "#include <stdio.h>\nint main()\n{\nif(input==password){\n        if(time<21){\n            printf(\"Access Granted\\n\");\n        } else {\n            printf(\"Late Night - Access Limited\\n\");\n        }\n    } else {\n        printf(\"Wrong Password\\n\");\n    }\nreturn 0;\n}"
    };

    private static final String[] LABELS = {
        "P1-Rectangle", "P2-MultTable", "P3-NumTriangle", "P4-Checkerboard(ERR)",
        "P5-StarTriangle", "P8-Palindrome", "P9-WhileLoop", "P10-DigitSum",
        "P11-Reverse(/=)", "P14-Largest3", "P15-Grades", "P17-LeapYear",
        "P18-EvenOdd", "P19-BothPositive", "P21-Login(ERR)"
    };

    public static void main(String[] args) {
        System.out.println("=== Compiler Test Suite ===\n");
        int passed = 0, total = PROGRAMS.length;

        for (int p = 0; p < PROGRAMS.length; p++) {
            String label = LABELS[p];
            System.out.printf("%-25s ", label);
            try {
                // Phase 1
                Lexer lexer = new Lexer(PROGRAMS[p]);
                java.util.List<Token> tokens = lexer.tokenize();
                if (!lexer.getErrors().isEmpty()) {
                    System.out.println("LEXER ERR: " + lexer.getErrors().get(0));
                    continue;
                }

                // Phase 2
                Parser parser = new Parser(tokens);
                ASTNode ast = parser.parse();
                if (!parser.getErrors().isEmpty()) {
                    System.out.println("PARSE ERR: " + parser.getErrors().get(0));
                    continue;
                }

                // Phase 3
                SemanticAnalyzer sem = new SemanticAnalyzer();
                sem.analyze(ast);
                String semStatus = sem.getErrors().isEmpty() ? "OK" : "WARN(" + sem.getErrors().size() + " errs)";

                // Phase 4
                IntermediateCodeGen icg = new IntermediateCodeGen();
                icg.generate(ast);

                // Phase 5
                CodeOptimizer opt = new CodeOptimizer();
                opt.optimize(icg.getCode());

                // Phase 6
                TargetCodeGen target = new TargetCodeGen();
                target.generate(opt.getOptimized());

                System.out.printf("PASS  [Tokens:%d  TAC:%d  Opt:%d  ASM:%d  Sem:%s]%n",
                    tokens.size(), icg.getCode().size(), opt.getOptimized().size(),
                    target.getAssembly().size(), semStatus);
                passed++;
            } catch (Exception e) {
                System.out.println("FAIL: " + e.getMessage());
            }
        }
        System.out.printf("%n=== Results: %d/%d passed ===%n", passed, total);
    }
}
