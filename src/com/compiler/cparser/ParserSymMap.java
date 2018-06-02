package com.compiler.cparser;

public class ParserSymMap {
    public static String getVal(Integer key) {
        return ParserSym.terminalNames[key];
    }
}
