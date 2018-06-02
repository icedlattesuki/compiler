package com.compiler.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FuncCall extends Exp {
    private Exp var;
    private Arg args;
}
