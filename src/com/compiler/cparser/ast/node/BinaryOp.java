package com.compiler.cparser.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BinaryOp extends Exp {
    private int op;
    private Exp exp1;
    private Exp exp2;
}
