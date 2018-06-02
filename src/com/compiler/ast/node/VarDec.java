package com.compiler.ast.node;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VarDec extends AstNode {
    private String name;
    private List<Integer> lengths;
    private Exp exp;
    private VarDec next;
}
