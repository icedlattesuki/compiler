package com.compiler.ir.node;

import com.compiler.ir.operand.Operand;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConditionJump extends IrNode {
    private Operand operand1;
    private Operand operand2;
    private int op;
    private int x;
}
