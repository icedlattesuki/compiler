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
public class Arg extends IrNode {
    private Operand operand;
}
