package com.compiler.cparser.ast.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 抽象语法树节点定义
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class AstNode {
    private int line;
}









