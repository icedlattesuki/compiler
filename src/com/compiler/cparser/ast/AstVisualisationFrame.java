package com.compiler.cparser.ast;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import javax.swing.*;

public class AstVisualisationFrame extends JFrame {
    private mxGraph graph = new mxGraph();
    private Object defaultParent = graph.getDefaultParent();

    public Object addNode(String val, Object parent) {
        graph.getModel().beginUpdate();
        try {
            Object node = graph.insertVertex(defaultParent, null, val, 0, 0, 80, 30);

            if (parent != null) {
                graph.insertEdge(defaultParent, null, "", parent, node);
            }

            return node;
        } finally {
            graph.getModel().endUpdate();
        }
    }

    public void print() {
        graph.getModel().beginUpdate();
        try {
            mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);
            layout.setUseBoundingBox(false);
            layout.execute(defaultParent);
        } finally {
            graph.getModel().endUpdate();
        }

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        getContentPane().add(graphComponent);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 320);
        setVisible(true);
    }
}
