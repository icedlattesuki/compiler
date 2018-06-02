package com.compiler.test;

import com.compiler.cparser.ParserSymMap;
import com.compiler.lexer.Lexer;
import com.compiler.cparser.ParserSym;
import java_cup.runtime.Symbol;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class LexerTest {
    public static void main(String[] args) {
        try {
            Lexer lexer = new Lexer(new FileReader(args[0]));
            PrintWriter writer = new PrintWriter(args[1]);
            for (Symbol symbol = lexer.next_token();symbol.sym != ParserSym.EOF;symbol = lexer.next_token()) {
                int type = symbol.sym;

                if (type >= 35) {
                    writer.println(symbol.value);
                } else if (type <= 4){
                    writer.println(ParserSymMap.getVal(type - 1));
                } else if (type == 5) {
                    writer.println(ParserSymMap.getVal(33));
                } else {
                    writer.println(ParserSymMap.getVal(type - 2));
                }
            }
            lexer.yyclose();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
