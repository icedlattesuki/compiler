package com.compiler.semantic;

import com.compiler.cparser.ParserSym;
import com.compiler.cparser.ast.Ast;
import com.compiler.cparser.ast.node.Arg;
import com.compiler.cparser.ast.node.ArrIndex;
import com.compiler.cparser.ast.node.AstNode;
import com.compiler.cparser.ast.node.BasicType;
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
import com.compiler.cparser.ast.node.Specifier;
import com.compiler.cparser.ast.node.StructType;
import com.compiler.cparser.ast.node.TypeDef;
import com.compiler.cparser.ast.node.UnaryOp;
import com.compiler.cparser.ast.node.Var;
import com.compiler.cparser.ast.node.VarDec;
import com.compiler.cparser.ast.node.VarDef;
import com.compiler.cparser.ast.node.VarDefList;
import com.compiler.cparser.ast.node.WhileStmt;
import com.compiler.semantic.symbol.SymbolTable;
import com.compiler.semantic.type.Array;
import com.compiler.semantic.type.Basic;
import com.compiler.semantic.type.Field;
import com.compiler.semantic.type.Func;
import com.compiler.semantic.type.FuncParam;
import com.compiler.semantic.type.Struct;
import com.compiler.semantic.type.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 语义分析器定义
 */
public class SemanticAnalyzer {
    private Ast ast;
    private SymbolTable table;
    private Map<String, Type> map;
    private boolean flag;
    private Type currentReturnType;
    private ArrayList<Integer> offsetStack = new ArrayList<>();
    private int count = 0;

    public SemanticAnalyzer(Ast ast) {
        this.ast = ast;
        table = new SymbolTable();
        map = new HashMap<>();
        flag = true;
    }

    public SymbolTable getSymbolTable() {
        return table;
    }

    public void analyse() throws Exception {
        analyse(ast.getRoot());

        if (!flag) {
            throw new Exception("semantic error.");
        }
    }

    private void analyse(AstNode node) {
        if (node == null) {
            return;
        }

        if (node instanceof Body) {
            Body body = (Body) node;
            table.enterScope(null);
            offsetStack.add(0);
            defType(body.getTypeDefs());
            defVar(body.getVarDefs());
            defFunc(body.getFuncDefs());
            FuncDef funcDef = body.getFuncDefs();
            while (funcDef != null) {
                String name = funcDef.getDec().getName();
                Func func = (Func) table.get(name).getType();
                currentReturnType = func.getReturnType();
                analyse(funcDef);
                funcDef = (FuncDef) funcDef.getNext();
            }
        } else if (node instanceof FuncDef) {
            FuncDef funcDef = (FuncDef) node;
            table.enterScope(table.getCurrent());
            offsetStack.add(0);
            defVar(funcDef.getDec());
            analyse(funcDef.getBody());
            table.leaveScope();
            offsetStack.remove(offsetStack.size() - 1);
        } else if (node instanceof CompStmt) {
            CompStmt compStmt = (CompStmt) node;
            table.enterScope(table.getCurrent());
            offsetStack.add(0);
            defVar(compStmt.getVarDefs());
            analyse(compStmt.getStmts());
            table.leaveScope();
            offsetStack.remove(offsetStack.size() - 1);
            analyse(compStmt.getNext());
        } else if (node instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) node;
            if (!isLogic(analyseExp(ifStmt.getExp()))) {
                System.out.println("Error at Line " + ifStmt.getExp().getLine() + ": Invalid condition.");
                flag = false;
            }
            analyse(ifStmt.getThenStmt());
            analyse(ifStmt.getElseStmt());
            analyse(ifStmt.getNext());
        } else if (node instanceof WhileStmt) {
            WhileStmt whileStmt = (WhileStmt) node;
            if (!isLogic(analyseExp(whileStmt.getExp()))) {
                System.out.println("Error at Line " + whileStmt.getExp().getLine() + ": Invalid condition.");
                flag = false;
            }
            analyse(whileStmt.getStmt());
            analyse(whileStmt.getNext());
        } else if (node instanceof ForStmt) {
            ForStmt forStmt = (ForStmt) node;
            analyseExp(forStmt.getExp1());
            if (!isLogic(analyseExp(forStmt.getExp2()))) {
                System.out.println("Error at Line " + forStmt.getExp2().getLine() + ": Invalid condition.");
                flag = false;
            }
            analyseExp(forStmt.getExp3());
            analyse(forStmt.getStmt());
            analyse(forStmt.getNext());
        } else if (node instanceof ReturnStmt) {
            ReturnStmt returnStmt = (ReturnStmt) node;
            if (!isValidAssign(currentReturnType, analyseExp(returnStmt.getExp()))) {
                System.out.println("Error at Line " + returnStmt.getExp().getLine() + ": Type mismatched for return.");
                flag = false;
            }
        } else if (node instanceof ExpStmt) {
            ExpStmt expStmt = (ExpStmt) node;
            analyseExp(expStmt.getExp());
            analyse(expStmt.getNext());
        } else {
            System.out.println(node.getClass().getTypeName() + " is not be analysed!");
        }
    }

    private void defType(TypeDef typeDef) {
        while (typeDef != null) {
            defType(typeDef.getSpecifier());
            typeDef = (TypeDef) typeDef.getNext();
        }
    }

    private void defType(Specifier specifier) {
        if (specifier instanceof StructType) {
            StructType structType = (StructType) specifier;
            String name = structType.getName();

            if (map.get(name) != null) {
                System.out.println("Error at Line " + specifier.getLine() + ": Redefined struct '" + name + "'.");
                flag = false;
                return;
            }

            Struct struct = new Struct(name, null);
            map.put(name, struct);
            Field fields = getFields(structType.getFields());
            struct.setFields(fields);
        }
    }

    private Field getFields(VarDefList varDefList) {
        Field head = null;
        Field tail = null;

        while (varDefList != null) {
            Field field = getFields(varDefList.getVarDef());

            if (field != null) {
                if (head == null) {
                    head = field;
                    tail = field;
                } else {
                    tail.setNext(field);
                }

                while (tail.getNext() != null) {
                    tail = tail.getNext();
                }
            }

            varDefList = varDefList.getNext();
        }

        if (head == tail) {
            return head;
        }

        Map<String, Integer> map = new HashMap<>();
        map.put(head.getName(), 0);
        Field ptr1 = head.getNext();
        Field ptr2 = head;

        while (ptr1 != null) {
            if (map.get(ptr1.getName()) != null) {
                System.out.println("Error at Line " + ptr1.getLine() + ": Redefined field '" + ptr1.getName() + "'.");
                flag = false;
                ptr2.setNext(ptr1.getNext());
                ptr1 = ptr1.getNext();
            } else {
                map.put(ptr1.getName(), 0);
                ptr1 = ptr1.getNext();
                ptr2 = ptr2.getNext();
            }
        }

        return head;
    }

    private Field getFields(VarDef varDef) {
        Type type = getType(varDef.getSpecifier());
        if (type != null) {
            return getFields(varDef.getDecs(), type);
        } else {
            return null;
        }
    }

    private Field getFields(VarDec varDec, Type type) {
        Field head = null;
        Field tail = null;

        while (varDec != null) {
            Field field = null;

            if (varDec.getLengths() != null && varDec.getLengths().size() > 0) {
                field = new Field(varDec.getName(), getArray(type, varDec.getLengths()), null, varDec.getLine());
            } else {
                field = new Field(varDec.getName(), type, null, varDec.getLine());
            }

            if (head == null) {
                head = field;
                tail = field;
            } else {
                tail.setNext(field);
                tail = field;
            }

            varDec = varDec.getNext();
        }

        return head;
    }

    private Type getType(Specifier specifier) {
        if (specifier instanceof BasicType) {
            BasicType type = (BasicType) specifier;
            return new Basic(type.getType());
        } else if (specifier instanceof StructType) {
            StructType type = (StructType) specifier;
            String name = type.getName();

            if (map.get(name) == null) {
                System.out.println("Error at Line " + specifier.getLine() + ": Undefined struct '" + name + "'.");
                flag = false;
                return null;
            } else {
                return map.get(name);
            }
        }

        return null;
    }

    private Array getArray(Type type, List<Integer> list) {
        Array array = null;

        for (int i = list.size() - 1; i >= 0; i--) {
            array = new Array(list.get(i), type);
            type = array;
        }

        return array;
    }

    private void defVar(VarDef varDef) {
        while (varDef != null) {
            Type type = getType(varDef.getSpecifier());

            if (type != null) {
                defVar(varDef.getDecs(), type);
            }

            varDef = (VarDef) varDef.getNext();
        }
    }

    private void defVar(VarDec varDec, Type type) {
        while (varDec != null) {
            if (table.getInCurrentScope(varDec.getName()) != null) {
                System.out.println("Error at Line " + varDec.getLine() + ": Redefined variable '" + varDec.getName() + "'.");
                flag = false;
            } else {
                if (varDec.getLengths() != null && varDec.getLengths().size() > 0) {
                    type = getArray(type, varDec.getLengths());
                }

                table.put(varDec.getName(), type, offsetStack.get(offsetStack.size() - 1), count, getSize(type));
                offsetStack.set(offsetStack.size() - 1, offsetStack.get(offsetStack.size() - 1) + getSize(type));
                count++;
            }

            varDec = varDec.getNext();
        }
    }

    public void defVar(FuncDec funcDec) {
        String name = funcDec.getName();
        Func func = (Func) table.get(name).getType();
        FuncParam funcParam = func.getParams();

        while (funcParam != null) {
            table.put(funcParam.getName(), funcParam.getType(), offsetStack.get(offsetStack.size() - 1), count, getSize(funcParam.getType()));
            offsetStack.set(offsetStack.size() - 1, offsetStack.get(offsetStack.size() - 1) + getSize(funcParam.getType()));
            count++;
            funcParam = funcParam.getNext();
        }
    }

    public void defVar(VarDefList varDefList) {
        while (varDefList != null) {
            defVar(varDefList.getVarDef());
            varDefList = varDefList.getNext();
        }
    }

    private void defFunc(FuncDef funcDef) {
        while (funcDef != null) {
            String name = funcDef.getDec().getName();

            if (table.getInCurrentScope(name) != null) {
                System.out.println("Error at Line " + funcDef.getLine() + ": Redefined function '" + name + "'.");
                flag = false;
                funcDef = (FuncDef) funcDef.getNext();
                continue;
            }

            Type returnType = getType(funcDef.getSpecifier());
            FuncParam funcParam = getParam(funcDef.getDec().getParams());
            table.put(name, new Func(returnType, funcParam), 0, -1, 0);

            funcDef = (FuncDef) funcDef.getNext();
        }
    }

    private FuncParam getParam(Param param) {
        FuncParam head = null;
        FuncParam tail = null;

        while (param != null) {
            String name = param.getVar().getName();
            Type type = getType(param.getSpecifier());

            if (param.getVar().getLengths() != null && param.getVar().getLengths().size() > 0) {
                type = getArray(type, param.getVar().getLengths());
            }

            FuncParam funcParam = new FuncParam(name, type, null);

            if (head == null) {
                head = funcParam;
                tail = funcParam;
            } else {
                tail.setNext(funcParam);
            }

            param = param.getNext();
        }

        return head;
    }

    private Type analyseExp(Exp exp) {
        if (exp == null) {
            return null;
        }

        if (exp instanceof BinaryOp) {
            BinaryOp binaryOp = (BinaryOp) exp;
            switch (binaryOp.getOp()) {
                case ParserSym.ASSIGN: {
                    Type left = analyseExp(binaryOp.getExp1());
                    Type right = analyseExp(binaryOp.getExp2());
                    if (!isLeftValue(binaryOp.getExp1())) {
                        System.out.println("Error at Line " + binaryOp.getLine() + ": The left-hand side of an assignment must be a variable.");
                        flag = false;
                    } else if (!isValidAssign(left, right)) {
                        System.out.println("Error at Line " + binaryOp.getLine() + ": Type mismatch for assignment.");
                        flag = false;
                    }
                    return left;
                }
                case ParserSym.AND:
                case ParserSym.OR: {
                    Type left = analyseExp(binaryOp.getExp1());
                    Type right = analyseExp(binaryOp.getExp2());
                    if (!isLogic(left) || !isLogic(right)) {
                        System.out.println("Error at Line " + binaryOp.getLine() + ": The operand of && or || must be logical.");
                        flag = false;
                    }
                    return new Basic(ParserSym.INT);
                }
                case ParserSym.LT:
                case ParserSym.LE:
                case ParserSym.GT:
                case ParserSym.GE:
                case ParserSym.EQ:
                case ParserSym.NEQ: {
                    Type left = analyseExp(binaryOp.getExp1());
                    Type right = analyseExp(binaryOp.getExp2());
                    if (!isComparable(left, right)) {
                        System.out.println("Error at Line " + binaryOp.getLine() + ": Type mismatch for compare.");
                        flag = false;
                    }
                    return new Basic(ParserSym.INT);
                }
                case ParserSym.PLUS:
                case ParserSym.MINUS:
                case ParserSym.MUL:
                case ParserSym.DIV:
                case ParserSym.MOD: {
                    Type left = analyseExp(binaryOp.getExp1());
                    Type right = analyseExp(binaryOp.getExp2());
                    Type res = cal(left, right);
                    if (res == null) {
                        System.out.println("Error at Line " + binaryOp.getLine() + ": Type mismatch for calculation.");
                        flag = false;
                    }
                    return res;
                }
            }
        } else if (exp instanceof UnaryOp) {
            UnaryOp unaryOp = (UnaryOp) exp;
            switch (unaryOp.getOp()) {
                case ParserSym.MINUS: {
                    Type left = analyseExp(unaryOp.getExp());
                    if (!hasInvert(left)) {
                        System.out.println("Error at Line " + unaryOp.getLine() + ": Type mismatch for operator '-'.");
                        flag = false;
                    }
                    return left;
                }
                case ParserSym.NOT: {
                    Type left = analyseExp(unaryOp.getExp());
                    if (!isLogic(left)) {
                        System.out.println("Error at Line " + unaryOp.getLine() + ": Type mismatch for operator '!'.");
                        flag = false;
                    }
                    return new Basic(ParserSym.INT);
                }
            }
        } else if (exp instanceof FuncCall) {
            FuncCall funcCall = (FuncCall) exp;
            Exp exp1 = funcCall.getVar();
            if (!isVar(exp1)) {
                System.out.println("Error at Line " + funcCall.getLine() + ": Can not call function on an expression.");
                flag = false;
                return null;
            }
            String name = ((Var) exp1).getName();
            if (table.get(name) == null) {
                System.out.println("Error at Line " + funcCall.getLine() + ": Undefined function " + "'" + name + "'.");
                flag = false;
                return null;
            }
            Type type = table.get(name).getType();
            if (!(type instanceof Func)) {
                System.out.println("Error at Line " + funcCall.getLine() + ": '" + name + "'" + " is not a function.");
                flag = false;
                return null;
            }
            Func func = (Func) type;
            if (!isValidCall(funcCall.getArgs(), func.getParams())) {
                System.out.println("Error at Line " + funcCall.getLine() + ": Incorrect args on function " + " '" + name + "'.");
                flag = false;
            }
            return func.getReturnType();
        } else if (exp instanceof ArrIndex) {
            ArrIndex arrIndex = (ArrIndex) exp;
            Exp exp1 = arrIndex.getVar();
            Exp exp2 = arrIndex.getExp();
            if (!isVar(exp1)) {
                System.out.println("Error at Line " + arrIndex.getLine() + ": Can not get array value from an expression.");
                flag = false;
                return null;
            }
            String name = ((Var) exp1).getName();
            Type type = table.get(name).getType();
            if (!(type instanceof Array)) {
                System.out.println("Error at Line " + arrIndex.getLine() + ": '" + name + "'" + " is not an array.");
                flag = false;
                return null;
            }
            if (!isInteger(analyseExp(exp2))) {
                System.out.println("Error at Line " + arrIndex.getLine() + ": Type of index should be int.");
                flag = false;
            }
            Array array = (Array) type;
            while (array.getType() instanceof Array) {
                array = (Array) array.getType();
            }
            return array.getType();
        } else if (exp instanceof GetField) {
            GetField getField = (GetField) exp;
            Exp exp1 = getField.getVar1();
            Exp exp2 = getField.getVar2();
            if (!isVar(exp1) || !isVar(exp2)) {
                System.out.println("Error at Line " + getField.getLine() + ": Illegal use of operator '.'.");
                flag = false;
                return null;
            }
            String name = ((Var) exp1).getName();
            if (table.get(name) == null) {
                System.out.println("Error at Line " + getField.getLine() + ": Struct '" + name + "'" + " is not defined.");
                flag = false;
                return null;
            }
            Type type = table.get(name).getType();
            if (!(type instanceof Struct)) {
                System.out.println("Error at Line " + getField.getLine() + ": Illegal use of operator '.'.");
                flag = false;
                return null;
            }
            Struct struct = (Struct) type;
            String fieldName = ((Var) exp2).getName();
            Type res = hasField(struct, fieldName);
            if (res == null) {
                System.out.println("Error at Line " + getField.getLine() + ": Struct '" + struct.getName() + "' does not have field '" + fieldName + "'.");
                flag = false;
            }
            return res;
        } else if (exp instanceof Var) {
            String name = ((Var) exp).getName();
            if (table.get(name) == null) {
                System.out.println("Error at Line " + exp.getLine() + ": Undefined variable '" + name + "'.");
                flag = false;
                return null;
            }
            return table.get(name).getType();
        } else if (exp instanceof Literal) {
            Literal literal = (Literal) exp;
            switch (literal.getType()) {
                case ParserSym.INT_LITERAL: return new Basic(ParserSym.INT);
                case ParserSym.CHAR_LITERAL: return new Basic(ParserSym.CHAR);
                case ParserSym.FLOAT_LITERAL: return new Basic(ParserSym.FLOAT);
                case ParserSym.STRING_LITERAL: return new Array(0, new Basic(ParserSym.CHAR));
            }
        }

        System.out.println(exp.getClass().getTypeName() + " is not analysed!");
        return null;
    }

    private boolean isLeftValue(Exp exp) {
        return exp instanceof Var || exp instanceof ArrIndex || exp instanceof GetField;
    }

    private boolean isComparable(Type left, Type right) {
        return left instanceof Basic && right instanceof Basic;
    }

    private boolean isValidAssign(Type left, Type right) {
        if (left instanceof Basic) {
            if (!(right instanceof Basic)) {
                return false;
            }

            Basic left1 = (Basic) left;
            Basic right1 = (Basic) right;

            switch (left1.getType()) {
                case ParserSym.CHAR:
                case ParserSym.INT: {
                    return right1.getType() != ParserSym.FLOAT;
                }
                case ParserSym.FLOAT: {
                    return true;
                }
            }
        } else if (left instanceof Struct) {
            if (!(right instanceof Struct)) {
                return false;
            }

            Struct left1 = (Struct) left;
            Struct right1 = (Struct) right;

            return left1.getName().equals(right1.getName());
        } else if (left instanceof Array){
            if (!(right instanceof Array)) {
                return false;
            }

            Array left1 = (Array) left;
            Array right1 = (Array) right;

            while (left1.getType() instanceof Array && right1.getType() instanceof Array) {
                left1 = (Array) left1.getType();
                right1 = (Array) right1.getType();
            }

            if (left1.getType() instanceof Array || right1.getType() instanceof Array) {
                return false;
            } else {
                return isValidAssign(left1.getType(), right1.getType());
            }
        }

        return false;
    }

    private boolean isLogic(Type type) {
        return isInteger(type);
    }

    private Type cal(Type left, Type right) {
        if (!(left instanceof Basic && right instanceof Basic)) {
            return null;
        }

        Basic left1 = (Basic) left;
        Basic right1 = (Basic) right;

        if (left1.getType() == ParserSym.FLOAT || right1.getType() == ParserSym.FLOAT) {
            return new Basic(ParserSym.FLOAT);
        } else {
            return new Basic(ParserSym.INT);
        }
    }

    private boolean hasInvert(Type type) {
        return type instanceof Basic;
    }

    private boolean isVar(Exp exp) {
        return exp instanceof Var;
    }

    private boolean isInteger(Type type) {
        if (!(type instanceof Basic)) {
            return false;
        }

        return ((Basic) type).getType() == ParserSym.INT;
    }

    private Type hasField(Struct struct, String fieldName) {
        Field field = struct.getFields();

        while (field != null) {
            if (field.getName().equals(fieldName)) {
                return field.getType();
            }

            field = field.getNext();
        }

        return null;
    }

    private boolean isValidCall(Arg arg, FuncParam param) {
        while (arg != null && param != null) {
            if (!isValidAssign(param.getType(), analyseExp(arg.getExp()))) {
                return false;
            }

            arg = arg.getNext();
            param = param.getNext();
        }

        return arg == null && param == null;
    }

    public static int getSize(Type type) {
        if (type instanceof Basic) {
            Basic basic = (Basic) type;
            switch (basic.getType()) {
                case ParserSym.INT:
                case ParserSym.FLOAT: {
                    return 4;
                }
                case ParserSym.CHAR: {
                    return 1;
                }
            }
        } else if (type instanceof Struct) {
            Struct struct = (Struct) type;
            int res = 0;
            Field field = struct.getFields();
            while (field != null) {
                res += getSize(field.getType());
                field = field.getNext();
            }
            return res;
        } else if (type instanceof Array) {
            Array array = (Array) type;
            int res = array.getSize();
            while (array.getType() instanceof Array) {
                array = (Array) array.getType();
                res *= array.getSize();
            }
            return res * getSize(array.getType());
        }

        return 0;
    }
}
