package com.compiler.cparser.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FuncDef extends Def {
    private Specifier specifier;
    private FuncDec dec;
    private CompStmt body;
}
