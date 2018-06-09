package com.compiler.semantic.symbol;

import com.compiler.semantic.type.Type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SymbolInfo {
    private Type type;
    private String name;
    private int size;
}
