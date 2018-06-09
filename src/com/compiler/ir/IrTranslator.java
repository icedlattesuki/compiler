package com.compiler.ir;

import com.compiler.cparser.ParserSym;
import com.compiler.cparser.ast.Ast;
import com.compiler.cparser.ast.node.Arg;
import com.compiler.cparser.ast.node.ArrIndex;
import com.compiler.cparser.ast.node.AstNode;
import com.compiler.cparser.ast.node.BinaryOp;
import com.compiler.cparser.ast.node.Body;
import com.compiler.cparser.ast.node.CompStmt;
import com.compiler.cparser.ast.node.Exp;
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
import com.compiler.cparser.ast.node.Stmt;
import com.compiler.cparser.ast.node.UnaryOp;
import com.compiler.cparser.ast.node.Var;
import com.compiler.cparser.ast.node.VarDec;
import com.compiler.cparser.ast.node.VarDef;
import com.compiler.cparser.ast.node.VarDefList;
import com.compiler.cparser.ast.node.WhileStmt;
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
import com.compiler.ir.operand.Variable;
import com.compiler.semantic.SemanticAnalyzer;
import com.compiler.semantic.symbol.SymbolTable;
import com.compiler.semantic.type.Array;
import com.compiler.semantic.type.StructField;
import com.compiler.semantic.type.Struct;
import com.compiler.semantic.type.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 将AST转为中间代码
 */
public class IrTranslator {
    private IrNode head;
    private IrNode tail;
    private Ast ast;
    private SymbolTable table;
    private int labelCount;
    private int tempCount;

    public IrTranslator(Ast ast, SymbolTable table) {
        this.ast = ast;
        this.table = table;
        labelCount = 0;
        tempCount = 0;
    }

    public IrList translate() {
        translate(ast.getRoot());
        return new IrList(head, tail);
    }

    private void translate(AstNode node) {
        if (node instanceof Body) {
            translateBody((Body) node);
        } else if (node instanceof VarDef) {
            translateVarDef((VarDef) node);
        } else if (node instanceof FuncDef) {
            translateFuncDef((FuncDef) node);
        } else if (node instanceof CompStmt) {
            translateCompStmt((CompStmt) node);
        } else if (node instanceof ReturnStmt) {
            translateReturnStmt((ReturnStmt) node);
        } else if (node instanceof IfStmt) {
            translateIfStmt((IfStmt) node);
        } else if (node instanceof WhileStmt) {
            translateWhileStmt((WhileStmt) node);
        } else if (node instanceof ForStmt) {
            translateForStmt((ForStmt) node);
        } else if (node instanceof ExpStmt) {
            translateExpStmt((ExpStmt) node);
        }
    }

    private void translateBody(Body body) {
        translate(body.getVarDefs());
        translate(body.getFuncDefs());
    }

    private void translateVarDef(VarDef varDef) {
        while (varDef != null) {
            VarDec varDecs = varDef.getDecs();
            while (varDecs != null) {
                translateVarDec(varDecs);
                varDecs = varDecs.getNext();
            }
            varDef = (VarDef) varDef.getNext();
        }
    }

    private void translateFuncDef(FuncDef funcDef) {
        while (funcDef != null) {
            table.enterScope();
            translateFuncDec(funcDef.getDec());
            translate(funcDef.getBody());
            funcDef = (FuncDef) funcDef.getNext();
            table.leaveScope();
        }
    }

    private void translateCompStmt(CompStmt compStmt) {
        table.enterScope();
        VarDefList varDefList = compStmt.getVarDefs();
        while (varDefList != null) {
            translate(varDefList.getVarDef());
            varDefList = varDefList.getNext();
        }
        Stmt stmts = compStmt.getStmts();
        while (stmts != null) {
            translate(stmts);
            stmts = stmts.getNext();
        }
        table.leaveScope();
    }

    private void translateReturnStmt(ReturnStmt returnStmt) {
        String t = newTemp();
        translateExp(returnStmt.getExp(), t);
        insert(new Return(new Variable(t)));
        removeTemp();
    }

    private void translateIfStmt(IfStmt ifStmt) {
        int label1 = newLabel();
        int label2 = newLabel();
        int label3 = newLabel();
        translateCond(ifStmt.getExp(), label1, label2);
        insert(new Label(label1));
        translate(ifStmt.getThenStmt());
        insert(new Goto(label3));
        insert(new Label(label2));
        translate(ifStmt.getElseStmt());
        insert(new Label(label3));
    }

    private void translateWhileStmt(WhileStmt whileStmt) {
        int label1 = newLabel();
        int label2 = newLabel();
        int label3 = newLabel();
        insert(new Label(label1));
        translateCond(whileStmt.getExp(), label2, label3);
        insert(new Label(label2));
        translate(whileStmt.getStmt());
        insert(new Goto(label1));
        insert(new Label(label3));
    }

    private void translateForStmt(ForStmt forStmt) {
        int label1 = newLabel();
        int label2 = newLabel();
        int label3 = newLabel();
        translateExp(forStmt.getExp1(), null);
        insert(new Label(label1));
        translateCond(forStmt.getExp2(), label2, label3);
        insert(new Label(label2));
        translate(forStmt.getStmt());
        translateExp(forStmt.getExp3(), null);
        insert(new Goto(label1));
        insert(new Label(label3));
    }

    private void translateExpStmt(ExpStmt expStmt) {
        translateExp(expStmt.getExp(), null);
    }

    private void translateVarDec(VarDec varDec) {
        while (varDec != null) {
            String name = varDec.getName();
            Type type = table.get(name).getType();
            String varNo = table.get(name).getName();
            int size = table.get(name).getSize();
            if (type instanceof Array || type instanceof Struct) {
                insert(new Dec(varNo, size));
            }
            varDec = varDec.getNext();
        }
    }

    private void translateFuncDec(FuncDec funcDec) {
        String name = funcDec.getName();
        insert(new Function(name));
        Param param = funcDec.getParams();
        while (param != null) {
            insert(new Parameter(table.get(param.getVar().getName()).getName()));
            param = param.getNext();
        }
    }

    private void translateExp(Exp exp, String place) {
        if (exp instanceof Literal) {
            Literal literal = (Literal) exp;
            if (place != null) {
                insert(new Assign(new Variable(place), new Constant(literal.getType(), literal.getValue())));
            }
        } else if (exp instanceof Var) {
            Var var = (Var) exp;
            if (place != null) {
                insert(new Assign(new Variable(place), new Variable(table.get(var.getName()).getName())));
            }
        } else if (exp instanceof BinaryOp) {
            BinaryOp binaryOp = (BinaryOp) exp;
            switch (binaryOp.getOp()) {
                case ParserSym.ASSIGN: {
                    Exp exp1 = binaryOp.getExp1();
                    if (exp1 instanceof Var) {
                        Var var = (Var) exp1;
                        String name = table.get(var.getName()).getName();
                        String t = newTemp();
                        translateExp(binaryOp.getExp2(), t);
                        insert(new Assign(new Variable(name), new Variable(t)));
                        if (place != null) {
                            insert(new Assign(new Variable(place), new Variable(name)));
                        }
                        removeTemp();
                    } else if (exp1 instanceof ArrIndex) {
                        String t1 = newTemp();
                        String t2 = newTemp();
                        String t3 = newTemp();
                        translateExp(binaryOp.getExp2(), t1);
                        translateArrIndex((ArrIndex) exp1, t2, t3);
                        insert(new Assign(new Memory(t3), new Variable(t1)));
                        if (place != null) {
                            insert(new Assign(new Variable(place), new Memory(t3)));
                        }
                        removeTemp();
                        removeTemp();
                        removeTemp();
                    } else if (exp1 instanceof GetField) {
                        String t1 = newTemp();
                        String t2 = newTemp();
                        translateExp(binaryOp.getExp2(), t1);
                        translateGetField((GetField) exp1, t2);
                        insert(new Assign(new Memory(t2), new Variable(t1)));
                        if (place != null) {
                            insert(new Assign(new Variable(place), new Memory(t2)));
                        }
                        removeTemp();
                        removeTemp();
                    }
                } break;
                case ParserSym.PLUS:
                case ParserSym.MINUS:
                case ParserSym.MUL:
                case ParserSym.DIV:
                case ParserSym.MOD: {
                    String t1 = newTemp();
                    String t2 = newTemp();
                    translateExp(binaryOp.getExp1(), t1);
                    translateExp(binaryOp.getExp2(), t2);
                    if (place != null) {
                        insert(new BinaryOperator(binaryOp.getOp(), new Variable(t1), new Variable(t2), place));
                    }
                    removeTemp();
                    removeTemp();
                } break;
                case ParserSym.LT:
                case ParserSym.LE:
                case ParserSym.GT:
                case ParserSym.GE:
                case ParserSym.EQ:
                case ParserSym.NEQ:
                case ParserSym.AND:
                case ParserSym.OR: {
                    relop(exp, place);
                } break;
            }
        } else if (exp instanceof UnaryOp) {
            UnaryOp unaryOp = (UnaryOp) exp;
            switch (unaryOp.getOp()) {
                case ParserSym.MINUS: {
                    String t1 = newTemp();
                    translateExp(unaryOp.getExp(), t1);
                    if (place != null) {
                        String t2 = newTemp();
                        insert(new Assign(new Variable(t2), new Constant(ParserSym.INT_LITERAL, 0)));
                        insert(new BinaryOperator(ParserSym.MINUS, new Variable(t2), new Variable(t1), place));
                        removeTemp();
                    }
                    removeTemp();
                } break;
                case ParserSym.NOT: {
                    relop(exp, place);
                } break;
            }
        } else if (exp instanceof FuncCall) {
            FuncCall funcCall = (FuncCall) exp;
            Arg args = funcCall.getArgs();
            List<String> temps = new ArrayList<>();
            while (args != null) {
                String t = newTemp();
                translateExp(args.getExp(), t);
                temps.add(t);
                args = args.getNext();
            }
            for (int i = temps.size() - 1;i >= 0;i--) {
                insert(new Argument(new Variable(temps.get(i))));
                removeTemp();
            }
            insert(new FunctionCall(((Var) funcCall.getVar()).getName(), place));
        } else if (exp instanceof ArrIndex) {
            String t1 = newTemp();
            String t2 = newTemp();
            translateArrIndex((ArrIndex) exp, t1, t2);
            if (place != null) {
                insert(new Assign(new Variable(place), new Memory(t2)));
            }
            removeTemp();
            removeTemp();
        } else if (exp instanceof GetField) {
            String t = newTemp();
            translateGetField((GetField) exp, t);
            if (place != null) {
                insert(new Assign(new Variable(place), new Memory(t)));
            }
            removeTemp();
        }
    }

    private void translateCond(Exp exp, int labelTrue, int labelFalse) {
        if (exp instanceof BinaryOp) {
            BinaryOp binaryOp = (BinaryOp) exp;
            switch (binaryOp.getOp()) {
                case ParserSym.LT:
                case ParserSym.LE:
                case ParserSym.GT:
                case ParserSym.GE:
                case ParserSym.EQ:
                case ParserSym.NEQ: {
                    String t1 = newTemp();
                    String t2 = newTemp();
                    translateExp(binaryOp.getExp1(), t1);
                    translateExp(binaryOp.getExp2(), t2);
                    insert(new ConditionJump(new Variable(t1), new Variable(t2), binaryOp.getOp(), labelTrue));
                    insert(new Goto(labelFalse));
                    removeTemp();
                    removeTemp();
                    return;
                }
                case ParserSym.AND: {
                    int label = newLabel();
                    translateCond(binaryOp.getExp1(), label, labelFalse);
                    insert(new Label(label));
                    translateCond(binaryOp.getExp2(), labelTrue, labelFalse);
                    return;
                }
                case ParserSym.OR: {
                    int label = newLabel();
                    translateCond(binaryOp.getExp1(), labelTrue, label);
                    insert(new Label(label));
                    translateCond(binaryOp.getExp2(), labelTrue, labelFalse);
                    return;
                }
            }
        } else if (exp instanceof UnaryOp) {
            UnaryOp unaryOp = (UnaryOp) exp;
            if (unaryOp.getOp() == ParserSym.NOT) {
                translateCond(unaryOp.getExp(), labelFalse, labelTrue);
                return;
            }
        }

        String t1 = newTemp();
        String t2 = newTemp();
        translateExp(exp, t1);
        insert(new Assign(new Variable(t2), new Constant(ParserSym.INT_LITERAL, 0)));
        insert(new ConditionJump(new Variable(t1), new Variable(t2), ParserSym.NEQ, labelTrue));
        insert(new Goto(labelFalse));
        removeTemp();
    }

    private void relop(Exp exp, String place) {
        int label1 = newLabel();
        int label2 = newLabel();
        if (place != null) {
            insert(new Assign(new Variable(place), new Constant(ParserSym.INT_LITERAL, 0)));
        }
        translateCond(exp, label1, label2);
        insert(new Label(label1));
        if (place != null) {
            insert(new Assign(new Variable(place), new Constant(ParserSym.INT_LITERAL, 1)));
        }
        insert(new Label(label2));
    }

    private void translateArrIndex(ArrIndex arrIndex, String t1, String t2) {
        String name = table.get(((Var) arrIndex.getVar()).getName()).getName();
        translateExp(arrIndex.getExp(), t1);
        Array array = (Array) table.get(((Var) arrIndex.getVar()).getName()).getType();
        while (array.getType() instanceof Array) {
            array = (Array) array.getType();
        }
        int size = SemanticAnalyzer.getSize(array.getType());
        String t3 = newTemp();
        insert(new Assign(new Variable(t3), new Constant(ParserSym.INT_LITERAL, size)));
        insert(new BinaryOperator(ParserSym.MUL, new Variable(t1), new Variable(t3), t1));
        removeTemp();
        insert(new Assign(new Variable(t2), new Address(name)));
        insert(new BinaryOperator(ParserSym.PLUS, new Variable(t2), new Variable(t1), t2));
    }

    private void translateGetField(GetField getField, String t) {
        String name = table.get(((Var) getField.getVar1()).getName()).getName();
        String fieldName = ((Var) getField.getVar2()).getName();
        Struct struct = (Struct) table.get(((Var) getField.getVar1()).getName()).getType();
        StructField fields = struct.getFields();
        int offset = 0;
        while (fields != null) {
            if (fields.getName().equals(fieldName)) {
                break;
            }
            offset += SemanticAnalyzer.getSize(fields.getType());
            fields = fields.getNext();
        }
        insert(new Assign(new Variable(t), new Address(name)));
        String t1 = newTemp();
        insert(new Assign(new Variable(t1), new Constant(ParserSym.INT_LITERAL, offset)));
        insert(new BinaryOperator(ParserSym.PLUS, new Variable(t), new Variable(t1), t));
        removeTemp();
    }

    private void insert(IrNode node) {
        if (head == null) {
            head = node;
            tail = node;
        } else {
            tail.setNext(node);
            node.setPrev(tail);
            head.setPrev(node);
            node.setNext(head);
            tail = node;
        }
    }

    private String newTemp() {
        tempCount++;
        return "t" + tempCount;
    }

    private void removeTemp() {
        tempCount--;
    }

    private int newLabel() {
        labelCount++;
        return labelCount;
    }
}
