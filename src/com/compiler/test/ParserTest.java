package com.compiler.test;

import com.compiler.cparser.Parser;
import com.compiler.lexer.Lexer;
import java.io.FileReader;

public class ParserTest {
    public static void main(String[] args) {
        try {
            Parser parser = new Parser(new Lexer(new FileReader(args[0])));
            parser.parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
