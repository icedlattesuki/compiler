package com.compiler.lexer;

import java_cup.runtime.*;
import com.compiler.cparser.ParserSym;

%%

%public
%class Lexer
%unicode
%cupsym ParserSym
%cup
%line
%column

%{
    StringBuffer stringBuffer = new StringBuffer();
    int charLength = 0;
    Character character = null;

    public Symbol symbol(int type) {
        return new Symbol(type, yyline + 1, yycolumn);
    }

    public Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline + 1, yycolumn, value);
    }
%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = \s

/* comments */
BlockComment = "/*"(([^\*]*(\*[^\/])?)*)"*/"
LineComment = "//"{InputCharacter}*{LineTerminator}?
Comment = {BlockComment} | {LineComment}

Identifier = [:jletter:] [:jletterdigit:]*
Decimal = 0 | [1-9][0-9]*
Hex = 0[xX][0-9a-fA-F]+
Octal = 0[0-7]+
IntegerLiteral = [+-]?({Decimal} | {Hex} | {Octal})
Lnum = [0-9]+
Dnum = ([0-9]*\.{Lnum}) | ({Lnum}\.[0-9]*)
FloatLiteral = [+-]?(({Lnum} | {Dnum}) ([eE][+-]?{Lnum})?)

%state STRING
%state CHAR

%%

<YYINITIAL> {
    "struct"    {return symbol(ParserSym.STRUCT);}
    "int"       {return symbol(ParserSym.INT);}
    "float"     {return symbol(ParserSym.FLOAT);}
    "char"      {return symbol(ParserSym.CHAR);}
    "return"    {return symbol(ParserSym.RETURN);}
    "if"        {return symbol(ParserSym.IF);}
    "else"      {return symbol(ParserSym.ELSE);}
    "while"     {return symbol(ParserSym.WHILE);}
    "for"       {return symbol(ParserSym.FOR);}
}

<YYINITIAL> {
    "="         {return symbol(ParserSym.ASSIGN);}
    ">"         {return symbol(ParserSym.GT);}
    "<"         {return symbol(ParserSym.LT);}
    ">="        {return symbol(ParserSym.GE);}
    "<="        {return symbol(ParserSym.LE);}
    "=="        {return symbol(ParserSym.EQ);}
    "!="        {return symbol(ParserSym.NEQ);}
    "+"         {return symbol(ParserSym.PLUS);}
    "-"         {return symbol(ParserSym.MINUS);}
    "*"         {return symbol(ParserSym.MUL);}
    "/"         {return symbol(ParserSym.DIV);}
    "%"         {return symbol(ParserSym.MOD);}
    "&&"        {return symbol(ParserSym.AND);}
    "||"        {return symbol(ParserSym.OR);}
    "!"         {return symbol(ParserSym.NOT);}
    "."         {return symbol(ParserSym.DOT);}
    ","         {return symbol(ParserSym.COMMA);}
    "["         {return symbol(ParserSym.LB);}
    "]"         {return symbol(ParserSym.RB);}
    "("         {return symbol(ParserSym.LP);}
    ")"         {return symbol(ParserSym.RP);}
}

<YYINITIAL> {
    ";"         {return symbol(ParserSym.SEMI);}
    "{"         {return symbol(ParserSym.LC);}
    "}"         {return symbol(ParserSym.RC);}
}

<YYINITIAL> {
    {Identifier}    {return symbol(ParserSym.ID, yytext());}
    {IntegerLiteral}    {
                            String s = yytext();
                            int t = 1;
                            if (s.charAt(0) == '+') {
                                s = s.substring(1);
                            } else if (s.charAt(0) == '-') {
                                s = s.substring(1);
                                t = -1;
                            }
                            if (s.length() >= 3 && (s.substring(0, 2).equals("0x") || s.substring(0, 2).equals("0X"))) {
                                return symbol(ParserSym.INT_LITERAL, Integer.parseInt(s.substring(2), 16) * t);
                            } else if (s.length() >= 2 && s.charAt(0) == '0') {
                                return symbol(ParserSym.INT_LITERAL, Integer.parseInt(s.substring(1), 8) * t);
                            } else {
                                return symbol(ParserSym.INT_LITERAL, Integer.parseInt(s, 10) * t);
                            }
                        }
    {FloatLiteral}  {return symbol(ParserSym.FLOAT_LITERAL, Float.parseFloat(yytext()));}
    \"              {stringBuffer.setLength(0);yybegin(STRING);}
    \'              {character = null;charLength = 0;yybegin(CHAR);}
}

<YYINITIAL> {
    {Comment}       {/*ignore*/}
    {WhiteSpace}    {/*ignore*/}
}

<STRING> {
    \"              {yybegin(YYINITIAL);return symbol(ParserSym.STRING_LITERAL, stringBuffer.toString());}
    [^\n\r\"\\]+    {stringBuffer.append(yytext());}
    \n              {/*ignore*/}
    \r              {/*ignore*/}
    \\n             {stringBuffer.append('\n');}
    \\r             {stringBuffer.append('\r');}
    \\\"            {stringBuffer.append('\"');}
    \\\\            {stringBuffer.append('\\');}
}

<CHAR> {
    \'              {
                        if (charLength != 1) {
                            throw new Error("Illegal char literal <" + character + "> in line " + (yyline + 1));
                        } else {
                            yybegin(YYINITIAL);
                            return symbol(ParserSym.CHAR_LITERAL, character);
                        }
                     }
    [^\n\r\'\\]     {character = yytext().charAt(0);charLength++;}
    \n              {}
    \r              {}
    \\n             {character = '\n';charLength++;}
    \\r             {character = '\r';charLength++;}
    \\\'            {character = '\'';charLength++;}
    \\\\            {character = '\\';charLength++;}
}

.               {System.out.println("Illegal character <" + yytext() + "> in line " + (yyline + 1)); System.exit(1);}



