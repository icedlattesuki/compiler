package com.compiler.semantic.type;

import lombok.*;
import org.bytedeco.javacpp.LLVM;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class Type {
    private int line;
    private LLVM.LLVMTypeRef llvmtype;
}
