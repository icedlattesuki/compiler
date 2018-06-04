package com.compiler.cparser.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VarDefList extends AstNode {
    private VarDef varDef;
    private VarDefList next;
}
