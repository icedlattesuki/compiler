package com.compiler.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ForStmt extends Stmt {
    private Exp exp1;
    private Exp exp2;
    private Exp exp3;
    private Stmt stmt;
}
