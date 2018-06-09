package com.compiler.ir.node;

import com.compiler.ir.operand.Variable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BinaryOperator extends IrNode {
    private int op;
    private Variable operand1;
    private Variable operand2;
    private String result;
}
