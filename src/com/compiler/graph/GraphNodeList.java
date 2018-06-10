package com.compiler.graph;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GraphNodeList {
    private GraphNode head;
    private GraphNodeList next;
}
