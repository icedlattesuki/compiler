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
public class BinaryOp extends IrNode {
    private int op;
    private Operand operand1;
    private Operand operand2;
    private String result;
}
