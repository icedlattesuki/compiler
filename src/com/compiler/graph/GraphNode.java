package com.compiler.graph;
import com.compiler.graph.ins.Instr;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GraphNode {
    private Graph root;
    private int key;
    private GraphNodeList succs;
    private GraphNodeList preds;
    private Instr instr;
}
