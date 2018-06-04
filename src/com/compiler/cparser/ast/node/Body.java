package com.compiler.cparser.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Body extends AstNode {
    private TypeDef typeDefs;
    private VarDef varDefs;
    private FuncDef funcDefs;
}
