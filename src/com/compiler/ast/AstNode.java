package com.compiler.ast;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public abstract class AstNode {

}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
abstract class Type extends AstNode {
    private boolean isArray;
    private int length;
}

@Getter
@Setter
class BasicType extends Type {
    private int name;

    public BasicType(int name) {
        super(false, 0);
        this.name = name;
    }

    public BasicType(int name, int length) {
        super(true, length);
        this.name = name;
    }
}

@Getter
@Setter
class StructType extends Type {
    private String name;
    private StructField fields;
    private StructType next;

    public StructType(String name, StructField fields, StructType next) {
        super(false, 0);
        this.name = name;
        this.fields = fields;
        this.next = next;
    }

    public StructType(String name, StructField fields, StructType next, int length) {
        super(true, length);
        this.name = name;
        this.fields = fields;
        this.next = next;
    }
}

@Getter
@Setter
@AllArgsConstructor
class StructField extends AstNode {
    private Type type;
    private String name;
    private StructField next;
}

@Getter
@Setter
@AllArgsConstructor
class Var extends AstNode {
    private String name;
}

@Getter
@Setter
@AllArgsConstructor
class VarDec extends AstNode {
    private Type type;
    private Var var;
    private Exp exp;
    private VarDec next;
}

@Getter
@Setter
@AllArgsConstructor
class FuncDec extends AstNode {
    private String name;
    private Type returnType;
    private Param params;
    private Body body;
    private FuncDec next;
}

@Getter
@Setter
@AllArgsConstructor
class Param extends AstNode {
    private Var var;
    private Type type;
    private Param next;
}

abstract class Exp extends AstNode {

}

@Getter
@Setter
@AllArgsConstructor
class BinaryOp extends Exp {
    private int op;
    private Exp exp1;
    private Exp exp2;
}

@Getter
@Setter
@AllArgsConstructor
class UnaryOp extends Exp {
    private int op;
    private Exp exp;
}

@Getter
@Setter
@AllArgsConstructor
class ArrIndex extends Exp {
    private Var var;
    private Exp exp;
}

@Getter
@Setter
@AllArgsConstructor
class GetField extends Exp {
    private Var var1;
    private Var var2;
}

@Getter
@Setter
@AllArgsConstructor
class FuncCall extends Exp {
    private Var var;
    private FuncArg args;
}

@Getter
@Setter
@AllArgsConstructor
class FuncArg extends AstNode {
    private Exp exp;
    private FuncArg next;
}

@Getter
@Setter
@AllArgsConstructor
class VarExp extends Exp {
    private Var var;
}

@Getter
@Setter
@AllArgsConstructor
class Literal extends Exp {
    private int type;
    private Object value;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
abstract class Stmt extends AstNode {
    private Stmt next;
}

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class ExpStmt extends Stmt {
    private Exp exp;

    public ExpStmt(Exp exp, Stmt next) {
        this(exp);
        this.setNext(next);
    }
}

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class IfStmt extends Stmt {
    private Exp exp;
    private Stmt thenStmt;
    private Stmt elseStmt;

    public IfStmt(Exp exp, Stmt thenStmt, Stmt elseStmt, Stmt next) {
        this(exp, thenStmt, elseStmt);
        this.setNext(next);
    }
}

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class WhileStmt extends Stmt {
    private Exp exp;
    private Stmt stmt;

    public WhileStmt(Exp exp, Stmt stmt, Stmt next) {
        this(exp, stmt);
        this.setNext(next);
    }
}

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class ForStmt extends Stmt {
    private Exp exps1;
    private Exp exp;
    private Exp exps2;

    public ForStmt(Exp exps1, Exp exp, Exp exps2, Stmt next) {
        this(exps1, exp, exps2);
        this.setNext(next);
    }
}

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class ReturnStmt extends Stmt {
    private Exp exp;

    public ReturnStmt(Exp exp, Stmt next) {
        this(exp);
        this.setNext(next);
    }
}

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class CompStmt extends Stmt {
    private Body body;

    public CompStmt(Body body, Stmt next) {
        this(body);
        this.setNext(next);
    }
}

@Getter
@Setter
@AllArgsConstructor
class Body extends AstNode {
    private StructType structTypes;
    private VarDec varDecs;
    private FuncDec funcDecs;
    private Stmt stmts;
}











