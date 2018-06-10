package com.compiler;

import com.compiler.cparser.Parser;
import com.compiler.cparser.ast.Ast;
import com.compiler.exception.SemanticError;
import com.compiler.exception.SyntaxError;
import com.compiler.lexer.Lexer;
import com.compiler.llvmir.LLVMIR;
import com.compiler.semantic.SemanticAnalyzer;
import java.io.FileReader;

public class Main {
    public static void main(String[] args) {
        try {
            Parser parser = new Parser(new Lexer(new FileReader(args[0])));
            Ast ast = (Ast) parser.parse().value;
            ast.print();
            SemanticAnalyzer analyzer = new SemanticAnalyzer(ast);
            analyzer.analyse();
            LLVMIR irTranslator = new LLVMIR(ast);
            irTranslator.genCode();
            irTranslator.runPasses();
            irTranslator.verifyCode();
            irTranslator.dumpIRToFile(args[1]);
            irTranslator.dumpCode();
            irTranslator.execMain();
        } catch (SyntaxError e) {
            System.out.println(e.getMessage());
        } catch (SemanticError e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
