package com.compiler.semantic.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class StructField {
    private String name;
    private Type type;
    private StructField next;
    private int line;
}
