package com.compiler.ast;

public class AstUtil {
    public static AstNode constructBasicType(int name) {
        return new BasicType(name);
    }

    public static AstNode constructBasicTypeArr(int name, int length) {
        return new BasicType(name, length);
    }

    public static AstNode constructStructType(String name, AstNode fields, AstNode next) {
        return new StructType(name, (StructField) fields, (StructType) next);
    }

    public static AstNode constructStructTypeArr(String name, AstNode fields, AstNode next, int length) {
        return new StructType(name, (StructField) fields, (StructType) next, length);
    }

    public static AstNode constructStructField(AstNode type, String name, AstNode next) {
        return new StructField((Type)type, name, (StructField)next);
    }

    public static AstNode constructVar(String name) {
        return new Var(name);
    }

    public static AstNode constructVarDec(AstNode type, AstNode var, AstNode exp, AstNode next) {
        return new VarDec((Type)type, (Var)var, (Exp)exp, (VarDec)next);
    }

    public static AstNode constructFuncDec(String name, AstNode returnType, AstNode params, AstNode body, AstNode next) {
        return new FuncDec(name, (Type)returnType, (Param)params, (Body)body, (FuncDec)next);
    }

    public static AstNode constructParam(AstNode var, AstNode type, AstNode next) {
        return new Param((Var)var, (Type)type, (Param)next);
    }

    public static AstNode constructBinaryOp(int op, AstNode exp1, AstNode exp2) {
        return new BinaryOp(op, (Exp)exp1, (Exp)exp2);
    }

    public static AstNode constructUnaryOp(int op, AstNode exp) {
        return new UnaryOp(op, (Exp)exp);
    }

    public static AstNode constructArrIndex(AstNode var, AstNode exp) {
        return new ArrIndex(((VarExp)var).getVar(), (Exp) exp);
    }

    public static AstNode constructGetField(AstNode var1, AstNode var2) {
        return new GetField(((VarExp)var1).getVar(), ((VarExp)var2).getVar());
    }

    public static AstNode constructFuncCall(AstNode var, AstNode args) {
        return new FuncCall(((VarExp)var).getVar(), (FuncArg) args);
    }

    public static AstNode constructFuncArg(AstNode exp, AstNode next) {
        return new FuncArg((Exp) exp, (FuncArg) next);
    }

    public static AstNode constructVarExp(AstNode var) {
        return new VarExp((Var) var);
    }

    public static AstNode constructLiteral(int type, Object value) {
        return new Literal(type, value);
    }

    public static AstNode constructExpStmt(AstNode exp, AstNode next) {
        return new ExpStmt((Exp) exp, (Stmt) next);
    }

    public static AstNode constructIfStmt(AstNode exp, AstNode thenStmt, AstNode elseStmt, AstNode next) {
        return new IfStmt((Exp) exp, (Stmt) thenStmt, (Stmt) elseStmt, (Stmt) next);
    }

    public static AstNode constructWhileStmt(AstNode exp, AstNode stmt, AstNode next) {
        return new WhileStmt((Exp) exp, (Stmt) stmt, (Stmt) next);
    }

    public static AstNode constructForStmt(AstNode exps1, AstNode exp, AstNode exps2, AstNode next) {
        return new ForStmt((Exp) exps1, (Exp) exp, (Exp) exps2, (Stmt) next);
    }

    public static AstNode constructReturnStmt(AstNode exp, AstNode next) {
        return new ReturnStmt((Exp) exp, (Stmt) next);
    }

    public static AstNode constructCompStmt(AstNode body, AstNode next) {
        return new CompStmt((Body) body, (Stmt) next);
    }

    public static AstNode constructBody(AstNode structTypes, AstNode varDecs, AstNode funcDecs, AstNode stmts) {
        return new Body((StructType) structTypes, (VarDec) varDecs, (FuncDec) funcDecs, (Stmt) stmts);
    }
}
