package com.compiler.ir.node;

import com.compiler.ir.operand.Operand;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Assign extends IrNode {
    private Operand left;
    private Operand right;
}
