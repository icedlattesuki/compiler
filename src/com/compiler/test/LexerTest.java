package com.compiler.test;

import com.compiler.lexer.Lexer;
import com.compiler.cparser.ParserSym;
import java_cup.runtime.Symbol;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class LexerTest {

    private static Map<Integer, String> map = new HashMap<>();

    static {
        map.put(1, "struct");
        map.put(2, "int");
        map.put(3, "float");
        map.put(33, "char");
        map.put(4, "return");
        map.put(5, "if");
        map.put(6, "else");
        map.put(7, "while");
        map.put(8, "for");
        map.put(9, "=");
        map.put(10, ">");
        map.put(11, ">=");
        map.put(12, "<");
        map.put(13, "<=");
        map.put(14, "==");
        map.put(15, "!=");
        map.put(16, "+");
        map.put(17, "-");
        map.put(18, "*");
        map.put(19, "/");
        map.put(20, "%");
        map.put(21, "&&");
        map.put(22, "||");
        map.put(23, "!");
        map.put(24, ".");
        map.put(25, ",");
        map.put(26, ";");
        map.put(27, "[");
        map.put(28, "]");
        map.put(29, "(");
        map.put(30, ")");
        map.put(31, "{");
        map.put(32, "}");
    }

    public static void main(String[] args) {
        try {
            Lexer lexer = new Lexer(new FileReader(args[0]));
            PrintWriter writer = new PrintWriter(args[1]);
            for (Symbol symbol = lexer.next_token();symbol.sym != ParserSym.EOF;symbol = lexer.next_token()) {
                int type = symbol.sym;

                if (type >= 35) {
                    writer.println(symbol.value);
                } else if (type <= 4){
                    writer.println(map.get(type -1));
                } else if (type == 5) {
                    writer.println(map.get(33));
                } else {
                    writer.println(map.get(type - 2));
                }
            }
            lexer.yyclose();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
