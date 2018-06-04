package com.compiler.cparser.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ArrIndex extends Exp {
    private Exp var;
    private Exp exp;
}
