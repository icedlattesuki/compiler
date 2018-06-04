package com.compiler.cparser.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class StructType extends Specifier {
    private String name;
    private VarDefList fields;
}
