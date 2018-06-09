package com.compiler.assemble;

import java.io.PrintWriter;
import java.util.List;

public class AssembleList {
    private List<String> text;
    private List<String> data;
    private PrintWriter writer = new PrintWriter(System.out);

    public AssembleList(List<String> text, List<String> data) {
        this.text = text;
        this.data = data;
    }

    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

    public void print() {
        writer.println(".text");
        writer.println(".globl main");
        for (String s : text) {
            writer.println(s);
        }
        writer.println(".data");
        for (String s : data) {
            writer.println(s);
        }
        writer.flush();
    }
}
