package com.compiler.assemble;

import com.compiler.cparser.ParserSym;
import com.compiler.ir.IrList;
import com.compiler.ir.node.Argument;
import com.compiler.ir.node.Assign;
import com.compiler.ir.node.BinaryOperator;
import com.compiler.ir.node.FunctionCall;
import com.compiler.ir.node.ConditionJump;
import com.compiler.ir.node.Dec;
import com.compiler.ir.node.Function;
import com.compiler.ir.node.Parameter;
import com.compiler.ir.node.Goto;
import com.compiler.ir.node.IrNode;
import com.compiler.ir.node.Label;
import com.compiler.ir.node.Return;
import com.compiler.ir.operand.Address;
import com.compiler.ir.operand.Constant;
import com.compiler.ir.operand.Memory;
import com.compiler.ir.operand.Operand;
import com.compiler.ir.operand.Variable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将中间代码转为MIPS汇编
 */
public class AssembleTranslator {
    private IrList irList;
    private List<String> text = new ArrayList<>();
    private List<String> data = new ArrayList<>();
    private Map<String, String> regMap = new HashMap<>();
    private int count = 0;

    public AssembleTranslator(IrList irList) {
        this.irList = irList;
    }

    public AssembleList translate() {
        IrNode head = irList.getHead();
        IrNode tail = irList.getTail();

        while (head != tail) {
            translate(head);
            head = head.getNext();
        }

        translate(head);

        return new AssembleList(text, data);
    }

    private void translate(IrNode node) {
        if (node instanceof Label) {
            translateLabel((Label) node);
        } else if (node instanceof Function) {
            translateFunction((Function) node);
        } else if (node instanceof Assign) {
            translateAssign((Assign) node);
        } else if (node instanceof BinaryOperator) {
            translateBinaryOp((BinaryOperator) node);
        } else if (node instanceof ConditionJump) {
            translateConditionJump((ConditionJump) node);
        } else if (node instanceof Goto) {
            translateGoto((Goto) node);
        } else if (node instanceof Return) {
            translateReturn((Return) node);
        } else if (node instanceof Dec) {
            translateDec((Dec) node);
        } else if (node instanceof Argument) {
            translateArg((Argument) node);
        } else if (node instanceof Parameter) {
            translateParam((Parameter) node);
        } else if (node instanceof FunctionCall) {
            translateCall((FunctionCall) node);
        }
    }

    private void translateLabel(Label label) {
        text.add((label.getX() + ":"));
    }

    private void translateFunction(Function function) {
        text.add(function.getName() + ":");
    }

    private void translateAssign(Assign assign) {
        Operand operand1 = assign.getLeft();
        Operand operand2 = assign.getRight();

        if (operand1 instanceof Variable) {
            String reg1 = getReg(((Variable) operand1).getName());
            if (operand2 instanceof Variable) {
                String reg2 = getReg(((Variable) operand2).getName());
                text.add("move " + reg1 + ", " + reg2);
            } else if (operand2 instanceof Constant) {
                Constant constant = (Constant) operand2;
                switch (constant.getType()) {
                    case ParserSym.CHAR_LITERAL:
                    case ParserSym.INT_LITERAL: text.add("li " + reg1 + ", " + (Integer) constant.getValue()); break;
                    case ParserSym.FLOAT_LITERAL: text.add("li " + reg1 + ", " + (Float) constant.getValue()); break;
                }
            } else if (operand2 instanceof Address) {
                text.add("la " + reg1 + ", " + ((Address) operand2).getName());
            } else if (operand2 instanceof Memory) {
                String reg2 = getReg(((Memory) operand2).getName());
                text.add("lw " + reg1 + ", 0(" + reg2 + ")");
            }
        } else if (operand1 instanceof Memory) {
            String reg1 = getReg(((Memory) operand1).getName());
            String reg2 = getReg(((Variable) operand2).getName());
            text.add("sw " + reg2 + ", 0(" + reg1 + ")");
        }
    }

    private void translateBinaryOp(BinaryOperator binaryOperator) {
        Operand operand1 = binaryOperator.getOperand1();
        Operand operand2 = binaryOperator.getOperand2();
        String reg1 = getReg(((Variable) operand1).getName());
        String reg2 = getReg(((Variable) operand2).getName());
        String reg3 = getReg(binaryOperator.getResult());

        switch (binaryOperator.getOp()) {
            case ParserSym.PLUS: text.add("add " + reg3 + ", " + reg1 + ", " + reg2); break;
            case ParserSym.MINUS: text.add("sub " + reg3 + ", " + reg1 + ", " + reg2); break;
            case ParserSym.MUL: text.add("mul " + reg3 + ", " + reg1 + ", " + reg2); break;
            case ParserSym.DIV: {
                text.add("div " + reg1 + ", " + reg2);
                text.add("mflo " + reg3);
            } break;
            case ParserSym.MOD: {
                text.add("div " + reg1 + ", " + reg2);
                text.add("mfhi " + reg3);
            } break;
        }
    }

    private void translateConditionJump(ConditionJump conditionJump) {
        Variable operand1 = conditionJump.getOperand1();
        Variable operand2 = conditionJump.getOperand2();
        int x = conditionJump.getX();
        String reg1 = getReg(operand1.getName());
        String reg2 = getReg(operand2.getName());

        switch (conditionJump.getOp()) {
            case ParserSym.LE: text.add("ble " + reg1 + ", " + reg2 + ", " + x); break;
            case ParserSym.LT: text.add("blt " + reg1 + ", " + reg2 + ", " + x); break;
            case ParserSym.GE: text.add("bge " + reg1 + ", " + reg2 + ", " + x); break;
            case ParserSym.GT: text.add("bgt " + reg1 + ", " + reg2 + ", " + x); break;
            case ParserSym.EQ: text.add("beq " + reg1 + ", " + reg2 + ", " + x); break;
            case ParserSym.NEQ: text.add("bne " + reg1 + ", " + reg2 + ", " + x); break;
        }
    }

    private void translateGoto(Goto g) {
        text.add("j " + g.getX());
    }

    private void translateReturn(Return r) {
        Variable operand = (Variable) r.getOperand();
        String reg = getReg(operand.getName());
        text.add("move $v0, " + reg);
        text.add("jr $ra");
    }

    private void translateDec(Dec dec) {
        data.add(dec.getName() + ": .space " + dec.getSize());
    }

    private void translateArg(Argument argument) {

    }

    private void translateParam(Parameter parameter) {

    }

    private void translateCall(FunctionCall functionCall) {

    }

    private String getReg(String varName) {
        if (regMap.get(varName) == null) {
            regMap.put(varName, "$t" + count);
            count++;
        }
        return regMap.get(varName);
    }
}
