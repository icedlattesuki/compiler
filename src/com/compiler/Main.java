package com.compiler;

import com.compiler.cparser.Parser;
import com.compiler.cparser.ast.Ast;
import com.compiler.lexer.Lexer;
import com.compiler.semantic.SemanticAnalyzer;

import java.io.FileReader;

public class Main {
    public static void main(String[] args) {
        try {
            Parser parser = new Parser(new Lexer(new FileReader(args[0])));
            Ast ast = (Ast) parser.parse().value;
//            ast.print();
            SemanticAnalyzer analyzer = new SemanticAnalyzer(ast);
            analyzer.analyse();
        } catch (Exception e) {
            System.out.println("Compile fail because of " + e.getMessage());
        }
    }
}
