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
import com.compiler.exception.SemanticError;
import com.compiler.semantic.symbol.SymbolTable;
import com.compiler.semantic.type.Array;
import com.compiler.semantic.type.Basic;
import com.compiler.semantic.type.StructField;
import com.compiler.semantic.type.Func;
import com.compiler.semantic.type.FuncParam;
import com.compiler.semantic.type.Struct;
import com.compiler.semantic.type.Type;

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
    private Type currentReturnType;
    private int count = 1;

    public SemanticAnalyzer(Ast ast) {
        this.ast = ast;
        table = new SymbolTable();
        map = new HashMap<>();
    }

    public SymbolTable getSymbolTable() {
        return table;
    }

    public void analyse() {
        analyse(ast.getRoot());
    }

    private void analyse(AstNode node) {
        if (node instanceof Body) {
            analyseBody((Body) node);
        } else if (node instanceof FuncDef) {
            analyseFucDef((FuncDef) node);
        } else if (node instanceof CompStmt) {
            analyseCompStmt((CompStmt) node);
        } else if (node instanceof IfStmt) {
            analyseIfStmt((IfStmt) node);
        } else if (node instanceof WhileStmt) {
            analyseWhileStmt((WhileStmt) node);
        } else if (node instanceof ForStmt) {
            analyseForStmt((ForStmt) node);
        } else if (node instanceof ReturnStmt) {
            analyseReturnStmt((ReturnStmt) node);
        } else if (node instanceof ExpStmt) {
            analyseExpStmt((ExpStmt) node);
        }
    }

    private void analyseBody(Body body) {
        table.enterScope(null);
        defType(body.getTypeDefs());
        defVar(body.getVarDefs());
        defFunc(body.getFuncDefs());
        FuncDef funcDefs = body.getFuncDefs();
        while (funcDefs != null) {
            String name = funcDefs.getDec().getName();
            Func func = (Func) table.get(name).getType();
            currentReturnType = func.getReturnType();
            analyse(funcDefs);
            funcDefs = (FuncDef) funcDefs.getNext();
        }
    }

    private void analyseFucDef(FuncDef funcDef) {
        table.enterScope(table.getCurrent());
        defVar(funcDef.getDec());
        analyse(funcDef.getBody());
        table.leaveScope();
    }

    private void analyseCompStmt(CompStmt compStmt) {
        table.enterScope(table.getCurrent());
        defVar(compStmt.getVarDefs());
        analyse(compStmt.getStmts());
        table.leaveScope();
        analyse(compStmt.getNext());
    }

    private void analyseIfStmt(IfStmt ifStmt) {
        if (!isLogic(analyseExp(ifStmt.getExp()))) {
            error("Error at Line " + ifStmt.getExp().getLine() + ": Invalid condition.");
        }
        analyse(ifStmt.getThenStmt());
        analyse(ifStmt.getElseStmt());
        analyse(ifStmt.getNext());
    }

    private void analyseWhileStmt(WhileStmt whileStmt) {
        if (!isLogic(analyseExp(whileStmt.getExp()))) {
            error("Error at Line " + whileStmt.getExp().getLine() + ": Invalid condition.");
        }
        analyse(whileStmt.getStmt());
        analyse(whileStmt.getNext());
    }

    private void analyseForStmt(ForStmt forStmt) {
        analyseExp(forStmt.getExp1());
        if (!isLogic(analyseExp(forStmt.getExp2()))) {
            error("Error at Line " + forStmt.getExp2().getLine() + ": Invalid condition.");
        }
        analyseExp(forStmt.getExp3());
        analyse(forStmt.getStmt());
        analyse(forStmt.getNext());
    }

    private void analyseReturnStmt(ReturnStmt returnStmt) {
        if (!isValidAssign(currentReturnType, analyseExp(returnStmt.getExp()))) {
            error("Error at Line " + returnStmt.getExp().getLine() + ": Type mismatched for return.");
        }
    }

    private void analyseExpStmt(ExpStmt expStmt) {
        analyseExp(expStmt.getExp());
        analyse(expStmt.getNext());
    }

    private Type analyseExp(Exp exp) {
        if (exp instanceof BinaryOp) {
            BinaryOp binaryOp = (BinaryOp) exp;
            switch (binaryOp.getOp()) {
                case ParserSym.ASSIGN: {
                    Type left = analyseExp(binaryOp.getExp1());
                    Type right = analyseExp(binaryOp.getExp2());
                    if (!isLeftValue(binaryOp.getExp1())) {
                        error("Error at Line " + binaryOp.getLine() + ": The left-hand side of an assignment must be a variable.");
                    }
                    if (!isValidAssign(left, right)) {
                        error("Error at Line " + binaryOp.getLine() + ": Type mismatch for assignment.");
                    }
                    return left;
                }
                case ParserSym.AND:
                case ParserSym.OR: {
                    Type left = analyseExp(binaryOp.getExp1());
                    Type right = analyseExp(binaryOp.getExp2());
                    if (!isLogic(left) || !isLogic(right)) {
                        error("Error at Line " + binaryOp.getLine() + ": The operand of && or || must be logical.");
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
                        error("Error at Line " + binaryOp.getLine() + ": Type mismatch for compare.");
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
                        error("Error at Line " + binaryOp.getLine() + ": Type mismatch for calculation.");
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
                        error("Error at Line " + unaryOp.getLine() + ": Type mismatch for operator '-'.");
                    }
                    return left;
                }
                case ParserSym.NOT: {
                    Type left = analyseExp(unaryOp.getExp());
                    if (!isLogic(left)) {
                        error("Error at Line " + unaryOp.getLine() + ": Type mismatch for operator '!'.");
                    }
                    return new Basic(ParserSym.INT);
                }
            }
        } else if (exp instanceof FuncCall) {
            FuncCall funcCall = (FuncCall) exp;
            Exp exp1 = funcCall.getVar();
            if (!isVar(exp1)) {
                error("Error at Line " + funcCall.getLine() + ": Can not call function on an expression.");
            }
            String name = ((Var) exp1).getName();
            if (table.get(name) == null) {
                error("Error at Line " + funcCall.getLine() + ": Undefined function " + "'" + name + "'.");
            }
            Type type = table.get(name).getType();
            if (!(type instanceof Func)) {
                error("Error at Line " + funcCall.getLine() + ": '" + name + "'" + " is not a function.");
            }
            Func func = (Func) type;
            if (!isValidCall(funcCall.getArgs(), func.getParams())) {
                error("Error at Line " + funcCall.getLine() + ": Incorrect args on function " + " '" + name + "'.");
            }
            return func.getReturnType();
        } else if (exp instanceof ArrIndex) {
            ArrIndex arrIndex = (ArrIndex) exp;
            Exp exp1 = arrIndex.getVar();
            Exp exp2 = arrIndex.getExp();
            if (!isVar(exp1)) {
                error("Error at Line " + arrIndex.getLine() + ": Can not get array value from an expression.");
            }
            String name = ((Var) exp1).getName();
            Type type = table.get(name).getType();
            if (!(type instanceof Array)) {
                error("Error at Line " + arrIndex.getLine() + ": '" + name + "'" + " is not an array.");
            }
            if (!isInteger(analyseExp(exp2))) {
                error("Error at Line " + arrIndex.getLine() + ": Type of index should be int.");
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
                error("Error at Line " + getField.getLine() + ": Illegal use of operator '.'.");
            }
            String name = ((Var) exp1).getName();
            if (table.get(name) == null) {
                error("Error at Line " + getField.getLine() + ": Struct '" + name + "'" + " is not defined.");
            }
            Type type = table.get(name).getType();
            if (!(type instanceof Struct)) {
                error("Error at Line " + getField.getLine() + ": Illegal use of operator '.'.");
            }
            Struct struct = (Struct) type;
            String fieldName = ((Var) exp2).getName();
            Type res = hasField(struct, fieldName);
            if (res == null) {
                error("Error at Line " + getField.getLine() + ": Struct '" + struct.getName() + "' does not have field '" + fieldName + "'.");
            }
            return res;
        } else if (exp instanceof Var) {
            String name = ((Var) exp).getName();
            if (table.get(name) == null) {
                error("Error at Line " + exp.getLine() + ": Undefined variable '" + name + "'.");
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

        return null;
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
                error("Error at Line " + specifier.getLine() + ": Redefined struct '" + name + "'.");
            }

            Struct struct = new Struct(name, null);
            map.put(name, struct);
            StructField fields = getFields(structType.getFields());
            struct.setFields(fields);
        }
    }

    private StructField getFields(VarDefList varDefList) {
        StructField head = null;
        StructField tail = null;

        while (varDefList != null) {
            StructField structField = getFields(varDefList.getVarDef());

            if (structField != null) {
                if (head == null) {
                    head = structField;
                    tail = structField;
                } else {
                    tail.setNext(structField);
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
        StructField ptr1 = head.getNext();
        StructField ptr2 = head;

        while (ptr1 != null) {
            if (map.get(ptr1.getName()) != null) {
                error("Error at Line " + ptr1.getLine() + ": Redefined field '" + ptr1.getName() + "'.");
            }
            map.put(ptr1.getName(), 0);
            ptr1 = ptr1.getNext();
            ptr2 = ptr2.getNext();
        }

        return head;
    }

    private StructField getFields(VarDef varDef) {
        Type type = getType(varDef.getSpecifier());
        if (type != null) {
            return getFields(varDef.getDecs(), type);
        } else {
            return null;
        }
    }

    private StructField getFields(VarDec varDec, Type type) {
        StructField head = null;
        StructField tail = null;

        while (varDec != null) {
            StructField structField;

            if (varDec.getLengths() != null && varDec.getLengths().size() > 0) {
                structField = new StructField(varDec.getName(), getArray(type, varDec.getLengths()), null, varDec.getLine());
            } else {
                structField = new StructField(varDec.getName(), type, null, varDec.getLine());
            }

            if (head == null) {
                head = structField;
                tail = structField;
            } else {
                tail.setNext(structField);
                tail = structField;
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
                error("Error at Line " + specifier.getLine() + ": Undefined struct '" + name + "'.");
            }

            return map.get(name);
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
                error("Error at Line " + varDec.getLine() + ": Redefined variable '" + varDec.getName() + "'.");
            }

            if (varDec.getLengths() != null && varDec.getLengths().size() > 0) {
                type = getArray(type, varDec.getLengths());
            }

            table.put(varDec.getName(), type, count, getSize(type));
            count++;

            varDec = varDec.getNext();
        }
    }

    private void defVar(FuncDec funcDec) {
        String name = funcDec.getName();
        Func func = (Func) table.get(name).getType();
        FuncParam params = func.getParams();

        while (params != null) {
            table.put(params.getName(), params.getType(), count, getSize(params.getType()));
            count++;
            params = params.getNext();
        }
    }

    private void defVar(VarDefList varDefList) {
        while (varDefList != null) {
            defVar(varDefList.getVarDef());
            varDefList = varDefList.getNext();
        }
    }

    private void defFunc(FuncDef funcDef) {
        while (funcDef != null) {
            String name = funcDef.getDec().getName();

            if (table.getInCurrentScope(name) != null) {
                error("Error at Line " + funcDef.getLine() + ": Redefined function '" + name + "'.");
            }

            Type returnType = getType(funcDef.getSpecifier());
            FuncParam funcParam = getParam(funcDef.getDec().getParams());
            table.put(name, new Func(returnType, funcParam), -1, 0);

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
        StructField structField = struct.getFields();

        while (structField != null) {
            if (structField.getName().equals(fieldName)) {
                return structField.getType();
            }

            structField = structField.getNext();
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

    private void error(String message) {
        throw new SemanticError(message);
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
                    return 4;
                }
            }
        } else if (type instanceof Struct) {
            Struct struct = (Struct) type;
            int res = 0;
            StructField structField = struct.getFields();
            while (structField != null) {
                res += getSize(structField.getType());
                structField = structField.getNext();
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
