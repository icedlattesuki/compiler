package com.compiler.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Param extends AstNode {
    private Specifier specifier;
    private VarDec var;
    private Param next;
}
