package com.compiler.semantic.symbol;

import com.compiler.semantic.type.Type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bytedeco.javacpp.LLVM;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SymbolInfo {
    private Type type;
    private LLVM.LLVMValueRef value;
}
