package com.compiler.cparser.ast;

import com.compiler.cparser.ast.node.Arg;
import com.compiler.cparser.ast.node.ArrIndex;
import com.compiler.cparser.ast.node.AstNode;
import com.compiler.cparser.ast.node.BasicType;
import com.compiler.cparser.ast.node.BinaryOp;
import com.compiler.cparser.ast.node.Body;
import com.compiler.cparser.ast.node.CompStmt;
import com.compiler.cparser.ast.node.ExpStmt;
import com.compiler.cparser.ast.node.ForStmt;
import com.compiler.cparser.ast.node.FuncCall;
import com.compiler.cparser.ast.node.FuncDec;
import com.compiler.cparser.ast.node.FuncDef;
import com.compiler.cparser.ast.node.GetField;
import com.compiler.cparser.ast.node.IfStmt;
import com.compiler.cparser.ast.node.Literal;
import com.compiler.cparser.ast.node.Param;
import com.compiler.cparser.ast.node.ReturnStmt;
import com.compiler.cparser.ast.node.StructType;
import com.compiler.cparser.ast.node.TypeDef;
import com.compiler.cparser.ast.node.UnaryOp;
import com.compiler.cparser.ast.node.Var;
import com.compiler.cparser.ast.node.VarDec;
import com.compiler.cparser.ast.node.VarDef;
import com.compiler.cparser.ast.node.VarDefList;
import com.compiler.cparser.ast.node.WhileStmt;
import com.compiler.cparser.ParserSym;
import com.compiler.cparser.ParserSymMap;
import java.util.List;

/**
 * 抽象语法树定义
 */
public class Ast {
    private AstNode root;

    public Ast(AstNode root) {
        this.root = root;
    }

    public AstNode getRoot() {
        return  root;
    }

    public void print() {
        AstVisualisationFrame frame = new AstVisualisationFrame();
        preOrderTraversal(root, null, frame);
        frame.print();
    }

    private void preOrderTraversal(AstNode node, Object parent, AstVisualisationFrame frame) {
        if (node == null) {
            return;
        }

        if (node instanceof Body) {
            Object o = frame.addNode("root", parent);
            Body body = (Body) node;
            preOrderTraversal(body.getTypeDefs(), o, frame);
            preOrderTraversal(body.getVarDefs(), o, frame);
            preOrderTraversal(body.getFuncDefs(), o, frame);
        } else if (node instanceof TypeDef) {
            Object o = frame.addNode("type_def", parent);
            TypeDef def = (TypeDef) node;
            preOrderTraversal(def.getSpecifier(), o, frame);
            preOrderTraversal(def.getNext(), o, frame);
        } else if (node instanceof VarDef) {
            Object o = frame.addNode("var_def", parent);
            VarDef def = (VarDef) node;
            preOrderTraversal(def.getSpecifier(), o, frame);
            preOrderTraversal(def.getDecs(), o, frame);
            preOrderTraversal(def.getNext(), o, frame);
        } else if (node instanceof FuncDef) {
            Object o = frame.addNode("func_def", parent);
            FuncDef def = (FuncDef) node;
            preOrderTraversal(def.getSpecifier(), o, frame);
            preOrderTraversal(def.getDec(), o, frame);
            preOrderTraversal(def.getBody(), o, frame);
            preOrderTraversal(def.getNext(), o, frame);
        } else if (node instanceof BasicType) {
            Object o = frame.addNode("basic_type", parent);
            BasicType type = (BasicType) node;
            frame.addNode(ParserSymMap.getVal(type.getType()), o);
        } else if (node instanceof StructType) {
            Object o = frame.addNode("struct_type", parent);
            StructType type = (StructType) node;
            frame.addNode(type.getName(), o);
            preOrderTraversal(type.getFields(), o, frame);
        } else if (node instanceof VarDec) {
            Object o = frame.addNode("var_dec", parent);
            VarDec dec = (VarDec) node;
            frame.addNode(dec.getName(), o);
            List<Integer> lengths = dec.getLengths();
            if (lengths != null) {
                Object o1 = o;
                for (int i = 0;i < lengths.size();i++) {
                    o1 = frame.addNode(Integer.toString(lengths.get(i)), o1);
                }
            }
            preOrderTraversal(dec.getExp(), o, frame);
            preOrderTraversal(dec.getNext(), o, frame);
        } else if (node instanceof FuncDec) {
            Object o = frame.addNode("func_dec", parent);
            FuncDec dec = (FuncDec) node;
            frame.addNode(dec.getName(), o);
            preOrderTraversal(dec.getParams(), o, frame);
        } else if (node instanceof Param) {
            Object o = frame.addNode("param", parent);
            Param param = (Param) node;
            preOrderTraversal(param.getSpecifier(), o, frame);
            preOrderTraversal(param.getVar(), o, frame);
            preOrderTraversal(param.getNext(), o, frame);
        } else if (node instanceof CompStmt) {
            Object o = frame.addNode("comp_stmt", parent);
            CompStmt stmt = (CompStmt) node;
            preOrderTraversal(stmt.getVarDefs(), o, frame);
            preOrderTraversal(stmt.getStmts(), o, frame);
            preOrderTraversal(stmt.getNext(), o, frame);
        } else if (node instanceof VarDefList) {
            Object o = frame.addNode("var_def_list", parent);
            VarDefList list = (VarDefList) node;
            preOrderTraversal(list.getVarDef(), o, frame);
            preOrderTraversal(list.getNext(), o, frame);
        } else if (node instanceof ReturnStmt) {
            Object o = frame.addNode("return_stmt", parent);
            ReturnStmt stmt = (ReturnStmt) node;
            preOrderTraversal(stmt.getExp(), o, frame);
            preOrderTraversal(stmt.getNext(), o, frame);
        } else if (node instanceof IfStmt) {
            Object o = frame.addNode("if_stmt", parent);
            IfStmt stmt = (IfStmt) node;
            preOrderTraversal(stmt.getExp(), o, frame);
            preOrderTraversal(stmt.getThenStmt(), o, frame);
            preOrderTraversal(stmt.getElseStmt(), o, frame);
            preOrderTraversal(stmt.getNext(), o, frame);
        } else if (node instanceof WhileStmt) {
            Object o = frame.addNode("while_stmy", parent);
            WhileStmt stmt = (WhileStmt) node;
            preOrderTraversal(stmt.getExp(), o, frame);
            preOrderTraversal(stmt.getStmt(), o, frame);
            preOrderTraversal(stmt.getNext(), o, frame);
        } else if (node instanceof ForStmt) {
            Object o = frame.addNode("for_stmt", parent);
            ForStmt stmt = (ForStmt) node;
            preOrderTraversal(stmt.getExp1(), o, frame);
            preOrderTraversal(stmt.getExp2(), o, frame);
            preOrderTraversal(stmt.getExp3(), o, frame);
            preOrderTraversal(stmt.getStmt(), o, frame);
            preOrderTraversal(stmt.getNext(), o, frame);
        } else if (node instanceof ExpStmt) {
            Object o = frame.addNode("exp_stmt", parent);
            ExpStmt stmt = (ExpStmt) node;
            preOrderTraversal(stmt.getExp(), o, frame);
            preOrderTraversal(stmt.getNext(), o, frame);
        } else if (node instanceof BinaryOp) {
            Object o = frame.addNode("binary_node", parent);
            BinaryOp op = (BinaryOp) node;
            preOrderTraversal(op.getExp1(), o, frame);
            frame.addNode(ParserSymMap.getVal(op.getOp()), o);
            preOrderTraversal(op.getExp2(), o, frame);
        } else if (node instanceof UnaryOp) {
            Object o = frame.addNode("unary_op", parent);
            UnaryOp op = (UnaryOp) node;
            frame.addNode(ParserSymMap.getVal(op.getOp()), o);
            preOrderTraversal(op.getExp(), o, frame);
        } else if (node instanceof FuncCall) {
            Object o = frame.addNode("func_call", parent);
            FuncCall call = (FuncCall) node;
            preOrderTraversal(call.getVar(), o, frame);
            preOrderTraversal(call.getArgs(), o, frame);
        } else if (node instanceof ArrIndex) {
            Object o = frame.addNode("arr_index", parent);
            ArrIndex index = (ArrIndex) node;
            preOrderTraversal(index.getVar(), o, frame);
            preOrderTraversal(index.getExp(), o, frame);
        } else if (node instanceof GetField) {
            Object o = frame.addNode("get_field", parent);
            GetField getField = (GetField) node;
            preOrderTraversal(getField.getVar1(), o, frame);
            preOrderTraversal(getField.getVar2(), o, frame);
        } else if (node instanceof Var) {
            Object o = frame.addNode("var", parent);
            Var var = (Var) node;
            frame.addNode(var.getName(), o);
        } else if (node instanceof Literal) {
            Object o = frame.addNode("literal", parent);
            Literal literal = (Literal) node;
            frame.addNode(ParserSymMap.getVal(literal.getType()), o);
            switch (literal.getType()) {
                case ParserSym.INT_LITERAL: frame.addNode(Integer.toString((Integer) literal.getValue()), o); break;
                case ParserSym.FLOAT_LITERAL: frame.addNode(Float.toString((Float) literal.getValue()), o); break;
                case ParserSym.CHAR_LITERAL: frame.addNode(Character.toString((Character) literal.getValue()), o); break;
                case ParserSym.STRING_LITERAL: frame.addNode((String) literal.getValue(), o); break;
            }
        } else if (node instanceof Arg) {
            Object o = frame.addNode("arg", parent);
            Arg arg = (Arg) node;
            preOrderTraversal(arg.getExp(), o, frame);
            preOrderTraversal(arg.getNext(), o, frame);
        } else {
            System.out.println(node.getClass().getTypeName());
        }
    }
}
