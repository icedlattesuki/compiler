package com.compiler;

import com.compiler.cparser.Parser;
import com.compiler.cparser.ast.Ast;
import com.compiler.ir.IrList;
import com.compiler.ir.IrTranslator;
import com.compiler.lexer.Lexer;
import com.compiler.semantic.SemanticAnalyzer;

import java.io.FileReader;
import java.io.PrintWriter;

public class Main {
    public static void main(String[] args) {
        try {
            Parser parser = new Parser(new Lexer(new FileReader(args[0])));
            Ast ast = (Ast) parser.parse().value;
//            ast.print();
            SemanticAnalyzer analyzer = new SemanticAnalyzer(ast);
            analyzer.analyse();
            IrTranslator translator = new IrTranslator(ast, analyzer.getSymbolTable());
            IrList irList = translator.translate();
            irList.setPrinter(new PrintWriter(args[1]));
            irList.print();
        } catch (Exception e) {
            System.out.println("Compile fail because of " + e.getMessage());
        }
    }
}
