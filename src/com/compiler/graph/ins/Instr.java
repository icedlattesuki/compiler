package com.compiler.graph.ins;
import com.compiler.graph.TempVarList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class Instr {
    private TempVarList uses;
    private TempVarList defs;
    private TempVarList in;
    private TempVarList out;
}
