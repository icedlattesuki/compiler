package com.compiler.semantic.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Func extends Type {
    private Type returnType;
    private FuncParam params;
}
