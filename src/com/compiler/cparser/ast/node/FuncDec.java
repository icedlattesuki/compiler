package com.compiler.cparser.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FuncDec extends AstNode {
    private String name;
    private Param params;
}
