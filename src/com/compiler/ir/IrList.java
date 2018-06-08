package com.compiler.ir;

import com.compiler.cparser.Parser;
import com.compiler.cparser.ParserSym;
import com.compiler.cparser.ast.Ast;
import com.compiler.cparser.ast.node.VarDef;
import com.compiler.ir.node.Arg;
import com.compiler.ir.node.Assign;
import com.compiler.ir.node.BinaryOp;
import com.compiler.ir.node.Call;
import com.compiler.ir.node.ConditionJump;
import com.compiler.ir.node.Dec;
import com.compiler.ir.node.Function;
import com.compiler.ir.node.Goto;
import com.compiler.ir.node.IrNode;
import com.compiler.ir.node.Label;
import com.compiler.ir.node.Param;
import com.compiler.ir.node.Return;
import com.compiler.ir.operand.Address;
import com.compiler.ir.operand.Constant;
import com.compiler.ir.operand.Memory;
import com.compiler.ir.operand.Operand;
import com.compiler.ir.operand.Variable;
import com.compiler.semantic.symbol.SymbolTable;
import com.compiler.semantic.type.Func;
import com.sun.org.apache.bcel.internal.generic.GOTO;
import com.sun.org.apache.xpath.internal.functions.FuncTranslate;

import sun.tools.jconsole.ConnectDialog;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Deque;

import lombok.AllArgsConstructor;

/**
 * IR树定义
 */
public class IrList {
    private IrNode head;
    private IrNode tail;
    private PrintWriter writer = new PrintWriter(System.out);

    public IrList(IrNode head, IrNode tail) {
        this.head = head;
        this.tail = tail;
    }

    public void setPrinter(PrintWriter writer) {
        this.writer = writer;
    }

    public void print() {
        IrNode ptr = head;
        while (ptr != tail) {
            print(ptr);
            ptr = ptr.getNext();
        }
        print(ptr);
        writer.flush();
    }

    private void print(IrNode node) {
        if (node == null) {
            return;
        }

        if (node instanceof Label) {
            Label label = (Label) node;
            writer.println("LABEL " + label.getX() + " :");
        } else if (node instanceof Function) {
            Function function = (Function) node;
            writer.println("FUNCTION " + function.getName() + " :");
        } else if (node instanceof Assign) {
            Assign assign = (Assign) node;
            print(assign.getLeft());
            writer.print(" := ");
            print(assign.getRight());
            writer.println();
        } else if (node instanceof BinaryOp) {
            BinaryOp binaryOp = (BinaryOp) node;
            if (binaryOp.getResult() != null) {
                writer.print(binaryOp.getResult() + " := ");
            }
            print(binaryOp.getOperand1());
            switch (binaryOp.getOp()) {
                case ParserSym.PLUS: writer.print(" + "); break;
                case ParserSym.MINUS: writer.print(" - "); break;
                case ParserSym.MUL: writer.print(" * "); break;
                case ParserSym.DIV: writer.print(" / "); break;
                case ParserSym.MOD: writer.print(" % "); break;
            }
            print(binaryOp.getOperand2());
            writer.println();
        } else if (node instanceof Goto) {
            writer.println("GOTO " + ((Goto) node).getX());
        } else if (node instanceof ConditionJump) {
            ConditionJump conditionJump = (ConditionJump) node;
            writer.print("IF ");
            print(conditionJump.getOperand1());
            writer.print(" ");
            switch (conditionJump.getOp()) {
                case ParserSym.LT: writer.print("<"); break;
                case ParserSym.LE: writer.print("<="); break;
                case ParserSym.GT: writer.print(">"); break;
                case ParserSym.GE: writer.print(">="); break;
                case ParserSym.EQ: writer.print("=="); break;
                case ParserSym.NEQ: writer.print("!="); break;
            }
            writer.print(" ");
            print(conditionJump.getOperand2());
            writer.print(" GOTO " + conditionJump.getX());
            writer.println();
        } else if (node instanceof Return) {
            writer.print("RETURN ");
            print(((Return) node).getOperand());
            writer.println();
        } else if (node instanceof Dec) {
            Dec dec = (Dec) node;
            writer.println("DEC " + dec.getName() + " [" + dec.getSize() + "]");
        } else if (node instanceof Arg) {
            Arg arg = (Arg) node;
            writer.print("ARG ");
            print(arg.getOperand());
            writer.println();
        } else if (node instanceof Call) {
            Call call = (Call) node;
            if (call.getResult() != null) {
                writer.print(call.getResult() + " := ");
            }
            writer.println("CALL " + call.getName());
        } else if (node instanceof Param) {
            Param param = (Param) node;
            writer.println("PARAM " + param.getName());
        }
    }

    private void print(Operand operand) {
        if (operand instanceof Variable) {
            writer.print(((Variable) operand).getName());
        } else if (operand instanceof Constant) {
            Constant constant = (Constant) operand;
            writer.print("#");
            switch (constant.getType()) {
                case ParserSym.INT_LITERAL: {
                    writer.print(constant.getValue());
                } break;
                case ParserSym.CHAR_LITERAL: {
                    int a = (Character) constant.getValue();
                    writer.print(a);
                } break;
                case ParserSym.FLOAT_LITERAL: {
                    writer.print(constant.getValue());
                }
            }
        } else if (operand instanceof Address) {
            writer.print("&" + ((Address) operand).getName());
        } else if (operand instanceof Memory) {
            writer.print("*" + ((Memory) operand).getName());
        }
    }
}
