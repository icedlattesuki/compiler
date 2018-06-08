package com.compiler.semantic.symbol;

import java.util.Map;
import java.util.TreeMap;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SymbolNode {
    private Map<String, SymbolInfo> map = new TreeMap<>();
    private SymbolNode left;
    private SymbolNode right;
    private SymbolNode sibling;
    private SymbolNode parent;
    private int color = 0;
}
