package com.compiler.ir.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
abstract public class IrNode {
    private IrNode next;
    private IrNode prev;
}
