package com.compiler.semantic.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FuncParam {
    private String name;
    private Type type;
    private FuncParam next;
}
