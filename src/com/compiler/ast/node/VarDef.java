package com.compiler.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VarDef extends Def {
    private Specifier specifier;
    private VarDec decs;
}
