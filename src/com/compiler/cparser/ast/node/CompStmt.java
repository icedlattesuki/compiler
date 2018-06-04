package com.compiler.cparser.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CompStmt extends Stmt {
    private VarDefList varDefs;
    private Stmt stmts;
}
