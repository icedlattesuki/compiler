package com.compiler.test;

import com.compiler.cparser.Parser;
import com.compiler.cparser.ast.Ast;
import com.compiler.cparser.ast.node.AstNode;
import com.compiler.lexer.Lexer;
import com.compiler.llvmir.*;

import java.io.FileReader;

public class LLVMIRTest {
    public static void main (String[] args) {

        try{
            Parser parser = new Parser(new Lexer(new FileReader(args[0])));
            Ast ast = (Ast)parser.parse().value;
            LLVMIR ir = new LLVMIR(ast);

            ir.test();

        }catch (Exception e){
            System.err.println(e.toString());
        }

    }
}
