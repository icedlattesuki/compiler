package com.compiler.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UnaryOp extends Exp {
    private int op;
    private Exp exp;
}
