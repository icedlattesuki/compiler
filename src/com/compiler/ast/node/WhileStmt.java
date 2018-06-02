package com.compiler.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WhileStmt extends Stmt {
    private Exp exp;
    private Stmt stmt;
}
