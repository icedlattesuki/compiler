package com.compiler.llvmir;

import com.compiler.cparser.ParserSym;
import com.compiler.cparser.ast.Ast;

import static org.bytedeco.javacpp.LLVM.*;

import com.compiler.cparser.ast.node.*;
import com.compiler.semantic.symbol.SymbolInfo;
import com.compiler.semantic.symbol.SymbolTable;
import com.compiler.semantic.type.*;
import org.bytedeco.javacpp.*;

import java.util.*;


public class LLVMIR {
    private LLVMModuleRef mod;
    private LLVMBuilderRef builder;
    private LLVMContextRef context;
    private SymbolTable structs;
    private SymbolTable variables;
    private SymbolTable functions;
    private LLVMValueRef currentFunction;
    private LLVMValueRef mainFunction;
    private boolean blockTerm;
    private BytePointer error;
    private AstNode root;

    public LLVMIR(Ast ast) {
        mod = LLVMModuleCreateWithName("my_module");
        context = LLVMGetModuleContext(mod);
        builder = LLVMCreateBuilderInContext(context);
        structs = new SymbolTable();
        variables = new SymbolTable();
        functions = new SymbolTable();
        root = ast.getRoot();
        blockTerm = false;


        error = new BytePointer((Pointer) null); // Used to retrieve messages from functions
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeDisassembler();
        LLVMInitializeNativeTarget();
    }

    public void runPasses() {
        LLVMPassManagerRef pass = LLVMCreatePassManager();

        LLVMAddConstantPropagationPass(pass);
        LLVMAddInstructionCombiningPass(pass);
        LLVMAddPromoteMemoryToRegisterPass(pass);
//         LLVMAddDemoteMemoryToRegisterPass(pass); // Demotes every possible value to memory
        LLVMAddGVNPass(pass);
        LLVMAddCFGSimplificationPass(pass);
        LLVMRunPassManager(pass, mod);

        LLVMDisposePassManager(pass);
    }

    public void execMain() throws Exception {
        LLVMExecutionEngineRef engine = new LLVMExecutionEngineRef();

        if (LLVMCreateJITCompilerForModule(engine, mod, 2, error) != 0) {
            System.err.println(error.getString());
            LLVMDisposeMessage(error);
            System.exit(-1);
        }

        LLVMGenericValueRef exec_args = LLVMCreateGenericValueOfInt(LLVMInt32Type(), 10, 0);
        LLVMGenericValueRef exec_res = LLVMRunFunction(engine, mainFunction, 1, exec_args);
        System.err.println();
        System.err.println("; Running main with JIT...");
        System.err.println("; Result: " + LLVMGenericValueToInt(exec_res, 0));
    }
    public void verifyCode() {
        LLVMVerifyModule(mod, LLVMAbortProcessAction, error);
        LLVMDisposeMessage(error); // Handler == LLVMAbortProcessAction -> No need to check errors
    }

    public void dumpCode() {
        LLVMDumpModule(mod);
    }

//    public void genMachineCode(String fileName){
//        LLVMInitializeAllTargetInfos();
//        LLVMInitializeAllTargets();
//        LLVMInitializeAllTargetMCs();
//        LLVMInitializeAllAsmParsers();
//        LLVMInitializeAllAsmPrinters();
//
//        BytePointer triple = LLVMGetDefaultTargetTriple();
//
//        LLVMGetTargetFromTriple(triple,,error)
//        LLVMCreateTargetMachine()
//        LLVMTargetMachineEmitToFile(,mod,fileName,1,error);
//        LLVMDisposeMessage(error);
//    }

    public void test() throws Exception {
        genCode(root);
        runPasses();
        verifyCode();
        dumpCode();
        execMain();
    }

    public void genCode(AstNode root) throws Exception {
        Body body = (Body) root;
        structs.enterScope(null);
        genCode(body.getTypeDefs());
        variables.enterScope(null);
        genCode(body.getVarDefs());
        functions.enterScope(null);
        genCode(body.getFuncDefs());
        functions.leaveScope();
        variables.leaveScope();
        structs.leaveScope();
    }
    // 生成类型定义
    private void genCode(TypeDef def) throws Exception {

        while (def != null) {
            AstNode node = def.getSpecifier();
            if (node instanceof StructType) {
                StructType type = (StructType) node;

                // Check fields
                VarDefList varDefList = type.getFields();
                List<LLVMTypeRef> llvmFieldsRef = new ArrayList<>();


                StructField head = null;
                StructField tail = null;
                int fieldCount = 0;

                // Process every define line
                while (varDefList != null) {
                    VarDef varDef = varDefList.getVarDef();

                    // Get line type
                    Type lineType;
                    Specifier spec = varDef.getSpecifier();
                    if (spec instanceof StructType) {
                        StructType structType = (StructType) spec;
                        lineType = structs.get(structType.getName()).getType();
                    } else if (spec instanceof BasicType) {
                        BasicType basicType = (BasicType) spec;
                        lineType = new Basic(basicType.getType());
                        lineType.setLlvmtype(getLLVMBasicType(basicType));
                    } else {
                        throw new Exception("Unknown Spec");
                    }

                    // Get every field
                    VarDec varDec = varDef.getDecs();
                    while (varDec != null) {
                        List<Integer> lengths = varDec.getLengths();
                        if (lengths != null) {
                            // LLVM Array type
                            LLVMTypeRef llvmarray = null;
                            Array array = null;
                            for (int i = lengths.size() - 1; i >= 0; i--) {
                                if (llvmarray != null) {
                                    llvmarray = LLVMArrayType(llvmarray, lengths.get(i));
                                    array = new Array(lengths.get(i), array);
                                    array.setLlvmtype(llvmarray);

                                } else {
                                    llvmarray = LLVMArrayType(lineType.getLlvmtype(), lengths.get(i));
                                    array = new Array(lengths.get(i), lineType);
                                    array.setLlvmtype(llvmarray);
                                }
                            }

                            // Update llvm StructField
                            llvmFieldsRef.add(llvmarray);
                            // Update StructField
                            if (head == null) {
                                head = new StructField(varDec.getName(), array, null, varDef.getLine());
                                tail = head;
                            } else {
                                tail.setNext(new StructField(varDec.getName(), array, null, varDef.getLine()));
                                tail = tail.getNext();
                            }
                        } else {
                            // Not array
                            // Update llvm StructField
                            llvmFieldsRef.add(lineType.getLlvmtype());
                            // Update StructField
                            if (head == null) {
                                head = new StructField(varDec.getName(), lineType, null, varDef.getLine());
                                tail = head;
                            } else {
                                tail.setNext(new StructField(varDec.getName(), lineType, null, varDef.getLine()));
                                tail = tail.getNext();
                            }

                        }
                        varDec = varDec.getNext();
                    }
                    varDefList = varDefList.getNext();
                }

                String structName = type.getName();

                // Construct array for pointerpointer
                LLVMTypeRef[] fieldArray = new LLVMTypeRef[llvmFieldsRef.size()];
                for (int i = 0; i < llvmFieldsRef.size(); i++) {
                    fieldArray[i] = llvmFieldsRef.get(i);
                }
                // Set llvm type in context
                LLVMTypeRef newType = LLVMStructCreateNamed(context, structName);
                LLVMStructSetBody(newType, new PointerPointer(fieldArray), llvmFieldsRef.size(), 1);

                // Record this struct
                Struct newStruct = new Struct(structName, head);
                newStruct.setLlvmtype(newType);
                structs.put(structName, newStruct,null);
            } else {
                throw new Exception(node.getClass().getTypeName());
            }
            def = (TypeDef) def.getNext();
        }
    }

    // 生成全局变量
    private void genCode(VarDef def) throws Exception {
        while (def != null) {
            Specifier spec = def.getSpecifier();
            VarDec dec = def.getDecs();

            while (dec != null) {
                List<Integer> lengths = dec.getLengths();
                if (lengths != null) {
                    // Array
                    Array array = null;
                    if (spec instanceof BasicType) {
                        BasicType type = (BasicType) spec;
                        Basic basic = new Basic(type.getType());
                        LLVMTypeRef llvmTypeRef = getLLVMBasicType(type);
                        basic.setLlvmtype(llvmTypeRef);
                        for (int i = lengths.size() - 1; i >= 0; i--) {
                            llvmTypeRef = LLVMArrayType(llvmTypeRef, lengths.get(i));
                            array = new Array(lengths.get(i), (i == lengths.size() - 1 ? basic : array));
                            array.setLlvmtype(llvmTypeRef);
                        }
                        LLVMValueRef value = genCode(array,dec.getName(),true);
                        variables.put(dec.getName(), array, value);
                    } else {
                        StructType type = (StructType) spec;
                        Struct struct = (Struct) structs.get(type.getName()).getType();
                        if (struct == null) throw new Exception("undefined struct type at " + type.getLine());
                        LLVMTypeRef llvmTypeRef = struct.getLlvmtype();
                        for (int i = lengths.size() - 1; i >= 0; i--) {
                            llvmTypeRef = LLVMArrayType(llvmTypeRef, lengths.get(i));
                            array = new Array(lengths.get(i), (i == lengths.size() - 1 ? struct : array));
                            array.setLlvmtype(llvmTypeRef);
                        }
                        LLVMValueRef value = genCode(array,dec.getName(),true);
                        variables.put(dec.getName(), array, value);
                    }
                } else {
                    // Not Array
                    if (spec instanceof BasicType) {
                        BasicType type = (BasicType) spec;
                        Basic basic = new Basic(type.getType());
                        LLVMTypeRef llvmTypeRef = getLLVMBasicType(type);
                        basic.setLlvmtype(llvmTypeRef);

                        LLVMValueRef value = genCode(basic,dec.getName(),true);
                        variables.put(dec.getName(), basic, value);
                    } else {
                        StructType type = (StructType) spec;
                        Struct struct = (Struct) structs.get(type.getName()).getType();
                        if (struct == null) throw new Exception("undefined struct type at " + type.getLine());

                        LLVMValueRef value = genCode(struct,dec.getName(),true);
                        variables.put(dec.getName(), struct, value);
                    }
                }
                dec = dec.getNext();
            }
            def = (VarDef) def.getNext();
        }
    }
    // 生成函数
    private void genCode(FuncDef def) throws Exception {
        while (def != null) {
            LLVMTypeRef llvmFuncType;
            LLVMValueRef llvmFunc;
            Func func;
            if (functions.get(def.getDec().getName()) == null) {
                // Get return type
                Specifier retSpec = def.getSpecifier();
                LLVMTypeRef llvmRetType;
                Type retType;
                if (retSpec instanceof BasicType) {
                    BasicType basicType = (BasicType) retSpec;
                    if (def.getDec().getName().equals("main") && ((BasicType) retSpec).getType() != ParserSym.INT) {
                        throw new Exception("main function must return int. line: " + def.getLine());
                    }
                    llvmRetType = getLLVMBasicType(basicType);
                    Basic basic = new Basic(basicType.getType());
                    basic.setLlvmtype(llvmRetType);
                    retType = basic;
                } else {
                    if (def.getDec().getName().equals("main")) {
                        throw new Exception("main function must return int. line: " + def.getLine());
                    }
                    StructType structType = (StructType) retSpec;
                    Struct struct = (Struct) structs.get(structType.getName()).getType();
                    if (struct == null) {
                        throw new Exception("type " + structType.getName() + " not defined. line: " + structType.getLine());
                    }
                    retType = struct;
                }
                // Get Params
                List<LLVMTypeRef> paramLLVMTypes = new ArrayList<>();
                FuncParam head = null;
                FuncParam tail = null;
                FuncDec funcDec = def.getDec();
                Param param = funcDec.getParams();
                while (param != null) {
                    Specifier paramSpec = param.getSpecifier();
                    Type paramType = getType(paramSpec);
                    VarDec varDec = param.getVar();
                    paramType = getArrayType(paramType, varDec.getLengths());
                    paramLLVMTypes.add(paramType.getLlvmtype());
                    FuncParam funcParam = new FuncParam(param.getVar().getName(), paramType, null);
                    if (head == null) {
                        head = funcParam;
                        tail = head;
                    } else {
                        tail.setNext(funcParam);
                        tail = funcParam;
                    }
                    param = param.getNext();
                }


                // Get LLVM param types
                LLVMTypeRef[] paramTypes = new LLVMTypeRef[paramLLVMTypes.size()];
                for (int i = 0; i < paramLLVMTypes.size(); i++) {
                    paramTypes[i] = paramLLVMTypes.get(i);
                }

                // Get LLVM func type
                if (def.getDec().getName().equals("main")) {
                    llvmFuncType = LLVMFunctionType(retType.getLlvmtype(), new PointerPointer(paramTypes), paramLLVMTypes.size(), 1);
                } else {
                    llvmFuncType = LLVMFunctionType(retType.getLlvmtype(), new PointerPointer(paramTypes), paramLLVMTypes.size(), 0);
                }

                llvmFunc = LLVMAddFunction(mod, funcDec.getName(), llvmFuncType);
                LLVMSetFunctionCallConv(llvmFunc, LLVMCCallConv);

                // Get Func
                func = new Func(retType, head);
                func.setLlvmtype(llvmFuncType);
                functions.put(funcDec.getName(), func, llvmFunc);
            } else {
                // Function already declared
                // Get Func
                SymbolInfo info = functions.get(def.getDec().getName());
                func = (Func) info.getType();
                llvmFuncType = func.getLlvmtype();
                llvmFunc = info.getValue();
            }
            currentFunction = llvmFunc;
            if (def.getDec().getName().equals("main")) {
                mainFunction = llvmFunc;
            }

            // Get Function Body
            if (def.getBody() != null) {
                variables.enterScope(variables.getCurrent());
                // Function entry
                LLVMBasicBlockRef entry = LLVMAppendBasicBlock(llvmFunc, "func_entry");
                LLVMPositionBuilderAtEnd(builder, entry);
                // Alloc all params
                int index = 0;
                FuncParam funcParam = func.getParams();
                while (funcParam != null) {
                    LLVMValueRef value = LLVMBuildAlloca(builder, funcParam.getType().getLlvmtype(), funcParam.getName());
                    LLVMBuildStore(builder, LLVMGetParam(llvmFunc, index), value);
                    variables.put(funcParam.getName(), funcParam.getType(), value);
                    funcParam = funcParam.getNext();
                    index++;
                }
                genCode(def.getBody());
                variables.leaveScope();
            }

            // Next Function
            def = (FuncDef) def.getNext();
        }

    }

    // 生成CompStmt
    private void genCode(CompStmt body) throws Exception {
        variables.enterScope(variables.getCurrent());
        // Get varDefs
        genCode(body.getVarDefs());
        genCode(body.getStmts());

        variables.leaveScope();
    }
    // 生成局部变量
    private void genCode(VarDefList varDefList) throws Exception {
        while (varDefList != null) {
            VarDef varDef = varDefList.getVarDef();
            Type varType = getType(varDef.getSpecifier());
            VarDec vardec = varDef.getDecs();
            LLVMValueRef value;
            while (vardec != null) {
                if (vardec.getLengths() != null) {
                    // Array will not be initialized
                    Type varDecType = getArrayType(varType, vardec.getLengths());
                    genCode((Array) varDecType, vardec.getName(), false);
                    // Allocate mem space
                    value = genCode(varDecType,vardec.getName(),false);
                    // Save value ref
                    variables.put(vardec.getName(), varDecType, value);
                } else {
                    if (varType instanceof Struct) {
                        // Struct will not be initialized
                        // Allocate mem space
                        value = genCode(varType,vardec.getName(),false);
                        // Save value ref
                        variables.put(vardec.getName(), varType, value);

                    } else {
                        // Basic type can be initialized
                        // Allocate mem space
                        value = genCode(varType,vardec.getName(),false);
                        // Save value ref
                        variables.put(vardec.getName(), varType, value);

                        if (vardec.getExp() != null) {
                            // Initialize using Exp
                            LLVMBuildStore(builder, genCode(vardec.getExp()), value);
                        }
                    }
                }
                vardec = vardec.getNext();
            }
            varDefList = varDefList.getNext();
        }
    }
    // 生成Stmt
    private void genCode(Stmt stmt) throws Exception {
        while (stmt != null) {
            if (stmt instanceof CompStmt) {
                genCode((CompStmt) stmt);
            } else if (stmt instanceof ExpStmt) {
                genCode((ExpStmt) stmt);
            } else if (stmt instanceof ForStmt) {
                genCode((ForStmt) stmt);
            } else if (stmt instanceof IfStmt) {
                genCode((IfStmt) stmt);
            } else if (stmt instanceof ReturnStmt) {
                genCode((ReturnStmt) stmt);
            } else if (stmt instanceof WhileStmt) {
                genCode((WhileStmt) stmt);
            } else {
                throw new Exception("unknown statement type. line: " + stmt.getLine());
            }
            stmt = stmt.getNext();
        }
    }

    private LLVMValueRef genCode(ExpStmt stmt) throws Exception {
        return genCode(stmt.getExp());
    }

    private void genCode(ForStmt stmt) throws Exception {
        LLVMBasicBlockRef loop = LLVMAppendBasicBlock(currentFunction, "FOR-COND");
        LLVMBasicBlockRef exec = LLVMAppendBasicBlock(currentFunction, "FOR-EXEC");
        LLVMBasicBlockRef loopend = LLVMAppendBasicBlock(currentFunction, "FOR-END");
        // Exp 1
        genCode(stmt.getExp1());
        LLVMBuildBr(builder, loop);

        // Exp 2
        LLVMPositionBuilderAtEnd(builder, loop);
        LLVMBuildCondBr(builder, genCode(stmt.getExp2()), exec, loopend);

        // Statements
        LLVMPositionBuilderAtEnd(builder, exec);
        genCode(stmt.getStmt());
        // Exp 3
        genCode(stmt.getExp3());
        LLVMBuildBr(builder, loop);

        // Exit loop
        LLVMPositionBuilderAtEnd(builder, loopend);

    }

    private void genCode(IfStmt stmt) throws Exception {
        LLVMBasicBlockRef ifthen = LLVMAppendBasicBlock(currentFunction, "IF-THEN");
        LLVMBasicBlockRef ifelse = LLVMAppendBasicBlock(currentFunction, "IF-ELSE");
        LLVMBasicBlockRef ifend = LLVMAppendBasicBlock(currentFunction, "IF-END");
        LLVMBuildCondBr(builder, genCode(stmt.getExp()), ifthen, ifelse);

        LLVMPositionBuilderAtEnd(builder, ifthen);
        genCode(stmt.getThenStmt());
        LLVMBuildBr(builder, ifend);

        LLVMPositionBuilderAtEnd(builder, ifelse);
        genCode(stmt.getElseStmt());
        LLVMBuildBr(builder, ifend);

        LLVMPositionBuilderAtEnd(builder, ifend);
    }

    private void genCode(ReturnStmt stmt) throws Exception {
        LLVMBuildRet(builder, genCode(stmt.getExp()));
    }

    private void genCode(WhileStmt stmt) throws Exception {
        LLVMBasicBlockRef loop = LLVMAppendBasicBlock(currentFunction, "WHILE-COND");
        LLVMBasicBlockRef exec = LLVMAppendBasicBlock(currentFunction, "WHILE-EXEC");
        LLVMBasicBlockRef loopend = LLVMAppendBasicBlock(currentFunction, "WHILE-END");

        LLVMBuildBr(builder, loop);

        // Condition
        LLVMPositionBuilderAtEnd(builder, loop);
        LLVMBuildCondBr(builder, genCode(stmt.getExp()), exec, loopend);

        // Statements
        LLVMPositionBuilderAtEnd(builder, exec);
        genCode(stmt.getStmt());
        LLVMBuildBr(builder, loop);

        // Exit loop
        LLVMPositionBuilderAtEnd(builder, loopend);
    }

    // 各类变量类型生成变量
    private LLVMValueRef genCode(Type type, String name, boolean global) throws Exception {
        if (type instanceof Array) {
            return genCode((Array) type, name, global);
        } else if (type instanceof Struct) {
            return genCode((Struct) type, name, global);
        } else if (type instanceof Basic) {
            return genCode((Basic) type, name, global);
        } else {
            throw new Exception("invalid type. line: " + type.getLine());
        }
    }
    private LLVMValueRef genCode(Array array, String name, boolean global) throws Exception {
        LLVMValueRef value;

        // Alloc array
        if (global) {
            value = LLVMAddGlobal(mod, array.getLlvmtype(), name);
            LLVMSetInitializer(value,LLVMConstNull(array.getLlvmtype()));
        } else {
            value = LLVMBuildAlloca(builder, array.getLlvmtype(), name);
        }

        return value;
    }

    private LLVMValueRef genCode(Struct struct, String name, boolean global) throws Exception {
        LLVMValueRef value;

        if (global) {
            value = LLVMAddGlobal(mod, struct.getLlvmtype(), name);
            LLVMSetInitializer(value,LLVMConstNull(struct.getLlvmtype()));
        } else {
            value = LLVMBuildAlloca(builder, struct.getLlvmtype(), name);
        }

        return value;
    }

    private LLVMValueRef genCode(Basic basic, String name, boolean global) throws Exception {
        LLVMValueRef value, addr;

        // Top level
        if (global) {
            value = LLVMAddGlobal(mod, getLLVMBasicType(new BasicType(basic.getType())), name);
            LLVMSetInitializer(value, LLVMConstNull(basic.getLlvmtype()));
        } else {
            value = LLVMBuildAlloca(builder, getLLVMBasicType(new BasicType(basic.getType())), name);
        }

        return value;
    }

    // 生成Exp中间值
    private LLVMValueRef genCode(Exp exp) throws Exception {
        if (exp instanceof FuncCall) {
            FuncCall funcCall = (FuncCall) exp;
            return genCode(funcCall);
        } else if (exp instanceof ArrIndex) {
            ArrIndex arrIndex = (ArrIndex) exp;
            return genCode(arrIndex);
        } else if (exp instanceof UnaryOp) {
            UnaryOp unaryOp = (UnaryOp) exp;
            return genCode(unaryOp);
        } else if (exp instanceof BinaryOp) {
            BinaryOp binaryOp = (BinaryOp) exp;
            return genCode(binaryOp);
        } else if (exp instanceof Var) {
            Var var = (Var) exp;
            return genCode(var);
        } else if (exp instanceof Literal) {
            Literal literal = (Literal) exp;
            return genCode(literal);
        } else if (exp instanceof GetField) {
            GetField getField = (GetField) exp;
            return genCode(getField);
        } else {
            throw new Exception("error Expression at line: " + exp.getLine());
        }

    }

    // 生成函数调用
    private LLVMValueRef genCode(FuncCall funcCall) throws Exception {
        // Find Function
        String funcName = ((Var) funcCall.getVar()).getName();
        SymbolInfo info = functions.get(funcName);
        if (info == null) {
            throw new Exception("Function " + funcName + " not defined. line: " + funcCall.getLine());
        }
        Func func = (Func) info.getType();

        // get args
        Arg arg = funcCall.getArgs();
        List<LLVMValueRef> argsList = new ArrayList<>();
        while (arg != null) {
            argsList.add(genCode(arg.getExp()));
            arg = arg.getNext();
        }

        // Reorganize args
        LLVMValueRef[] argsArray = new LLVMValueRef[argsList.size()];
        for (int i = 0; i < argsList.size(); i++) {
            argsArray[i] = argsList.get(i);
        }

        // Make call
        return LLVMBuildCall(builder, info.getValue(), new PointerPointer(argsArray), argsList.size(), "call_" + funcName);
    }

    // 生成Array右值
    private LLVMValueRef genCode(ArrIndex arrIndex) throws Exception {
        // Find Array
        String name = getArrName(arrIndex);

        LLVMValueRef ptr = genPtr(arrIndex);
        return LLVMBuildLoad(builder,ptr,"load_"+name);
    }

    private LLVMValueRef genCode(UnaryOp unaryOp) throws Exception {
        int op = unaryOp.getOp();
        switch (op) {
            case ParserSym.MINUS: {
                return LLVMBuildNeg(builder, genCode(unaryOp.getExp()), "neg_op");
            }
            case ParserSym.NOT: {
                return LLVMBuildNot(builder, genCode(unaryOp.getExp()), "not_op");
            }
            default:
                throw new Exception("Unknown unaryOp: " + op + ". line: " + unaryOp.getLine());
        }
    }

    private LLVMValueRef genCode(BinaryOp binaryOp) throws Exception {
        int op = binaryOp.getOp();
        switch (op) {
            case ParserSym.ASSIGN: {
                LLVMValueRef ptr = genPtr(binaryOp.getExp1());
                return LLVMBuildStore(builder, genCode(binaryOp.getExp2()), ptr);
            }
            case ParserSym.AND: {
                return LLVMBuildAnd(builder, genCode(binaryOp.getExp1()), genCode(binaryOp.getExp2()), "and");
            }
            case ParserSym.OR: {
                return LLVMBuildOr(builder, genCode(binaryOp.getExp1()), genCode(binaryOp.getExp2()), "or");
            }
            case ParserSym.GT: {
                return LLVMBuildICmp(builder, LLVMIntSGT, genCode(binaryOp.getExp1()), genCode(binaryOp.getExp2()), "gt");
            }
            case ParserSym.GE: {
                return LLVMBuildICmp(builder, LLVMIntSGE, genCode(binaryOp.getExp1()), genCode(binaryOp.getExp2()), "ge");
            }
            case ParserSym.LT: {
                return LLVMBuildICmp(builder, LLVMIntSLT, genCode(binaryOp.getExp1()), genCode(binaryOp.getExp2()), "lt");
            }
            case ParserSym.LE: {
                return LLVMBuildICmp(builder, LLVMIntSLE, genCode(binaryOp.getExp1()), genCode(binaryOp.getExp2()), "le");
            }
            case ParserSym.EQ: {
                return LLVMBuildICmp(builder, LLVMIntEQ, genCode(binaryOp.getExp1()), genCode(binaryOp.getExp2()), "eq");
            }
            case ParserSym.NEQ: {
                return LLVMBuildICmp(builder, LLVMIntNE, genCode(binaryOp.getExp1()), genCode(binaryOp.getExp2()), "neq");
            }
            case ParserSym.PLUS: {
                return LLVMBuildAdd(builder, genCode(binaryOp.getExp1()), genCode(binaryOp.getExp2()), "add");
            }
            case ParserSym.MINUS: {
                return LLVMBuildSub(builder, genCode(binaryOp.getExp1()), genCode(binaryOp.getExp2()), "sub");
            }
            case ParserSym.MUL: {
                return LLVMBuildMul(builder, genCode(binaryOp.getExp1()), genCode(binaryOp.getExp2()), "mul");
            }
            case ParserSym.DIV: {
                return LLVMBuildSDiv(builder, genCode(binaryOp.getExp1()), genCode(binaryOp.getExp2()), "div");
            }
            case ParserSym.MOD: {
                return LLVMBuildSRem(builder, genCode(binaryOp.getExp1()), genCode(binaryOp.getExp2()), "mod");
            }
            default:
                throw new Exception("invalid binary operation. line: " + binaryOp.getLine());
        }
    }

    private LLVMValueRef genCode(Var var) throws Exception {
        SymbolInfo info = variables.get(var.getName());
        if (info == null) {
            throw new Exception("undefined variable \"" + var.getName() + "\". line: " + var.getLine());
        }

        return LLVMBuildLoad(builder, info.getValue(), var.getName());
    }

    private LLVMValueRef genCode(Literal literal) throws Exception {
        int type = literal.getType();
        switch (type) {
            case ParserSym.INT_LITERAL: {
                return LLVMConstInt(LLVMInt32Type(), ((Integer) literal.getValue()).longValue(), 1);
            }
            case ParserSym.FLOAT_LITERAL: {
                return LLVMConstReal(LLVMFloatType(), ((Float) literal.getValue()).doubleValue());
            }
            case ParserSym.CHAR_LITERAL: {
                return LLVMConstInt(LLVMInt8Type(), ((Character) literal.getValue()).charValue(), 1);
            }
            case ParserSym.STRING_LITERAL: {
                String string = (String) literal.getValue();
                int length = string.length();
                LLVMValueRef[] array = new LLVMValueRef[length];
                for (int i = 0; i < length; i++) {
                    array[i] = LLVMConstInt(LLVMInt8Type(), string.charAt(i), 1);
                }
                return LLVMConstArray(LLVMInt8Type(), new PointerPointer(array), length);
            }
            default:
                throw new Exception("invalid literal type. line: " + literal.getLine());
        }
    }


    private LLVMValueRef genCode(GetField getField) throws Exception {
        LLVMValueRef ptr = genPtr(getField.getVar1());
        Type leftType = getExpType(getField.getVar1());
        String fieldName = ((Var)getField.getVar2()).getName();

        // Get field count
        if (leftType instanceof Basic) {
            throw new Exception("dot used in non struct type. line: " + getField.getLine());
        }
        Struct struct = (Struct) leftType;
        ptr = LLVMBuildStructGEP(builder, ptr, findFieldCount(struct.getFields(), fieldName), ((Struct) leftType).getName());
        return LLVMBuildLoad(builder, ptr, ((Struct) leftType).getName() + "." + fieldName);
    }

    // 各类左值
    private LLVMValueRef genPtr(Exp exp) throws Exception {
        if (exp instanceof ArrIndex) {
            ArrIndex arrIndex = (ArrIndex) exp;
            return genPtr(arrIndex);
        } else if (exp instanceof Var) {
            Var var = (Var) exp;
            return genPtr(var);
        } else if (exp instanceof GetField) {
            GetField getField = (GetField) exp;
            return genPtr(getField);
        } else {
            throw new Exception("error Lvalue type at line: " + exp.getLine());
        }
    }

    private LLVMValueRef genPtr(GetField getField) throws Exception {
        LLVMValueRef ptr = genPtr(getField.getVar1());
        Type leftType = getExpType(getField.getVar1());
        String fieldName = ((Var) getField.getVar2()).getName();
        // Get field count
        if (leftType instanceof Basic) {
            throw new Exception("dot used in non struct type. line: " + getField.getLine());
        }
        Struct struct = (Struct) leftType;
        return LLVMBuildStructGEP(builder, ptr, findFieldCount(struct.getFields(), fieldName), ((Struct) leftType).getName());
    }

    private LLVMValueRef genPtr(ArrIndex arrIndex) throws Exception {
        // Find Array
        String name = getArrName(arrIndex);
        List<LLVMValueRef> list=getArrayIndices(arrIndex);

//        SymbolInfo info = variables.get(name);
//        if (info == null) {
//            throw new Exception("Array " + name + " not defined. line: " + arrIndex.getLine());
//        }
//        // Get Pointer
//        Exp var = arrIndex.getVar();
//        while (var instanceof ArrIndex){
//            var=((ArrIndex) var).getVar();
//        }

        LLVMValueRef[] indices = new LLVMValueRef[list.size()];
        for (int i = 0; i < list.size(); i++) {
            indices[i]=list.get(i);
        }
        return LLVMBuildGEP(builder, genPtr(arrIndex.getVar()), new PointerPointer(indices), list.size(), "gep_array_" + name);
    }


    private LLVMValueRef genPtr(Var var) throws Exception {
        String name = var.getName();
        SymbolInfo info = variables.get(name);
        if (info == null) {
            throw new Exception("variable " + name + " not defined. line: " + var.getLine());
        }
        return info.getValue();
    }

    private int findFieldCount(StructField structField, String name) {
        int fieldcount = 0;
        while (structField != null) {
            if (structField.getName().equals(name)) {
                break;
            }
            fieldcount++;
            structField = structField.getNext();
        }
        return fieldcount;
    }
    private String getArrName(ArrIndex arrIndex) throws Exception{
        if(arrIndex.getVar() instanceof ArrIndex){
            return getArrName((ArrIndex)arrIndex.getVar());
        }else if(arrIndex.getVar() instanceof GetField){
            GetField getField = (GetField)arrIndex.getVar();
            return((Var)getField.getVar2()).getName();
        }else return ((Var)arrIndex.getVar()).getName();
    }
    private Type getExpType(Exp exp) throws Exception {
        if (exp instanceof GetField) {
            return getExpType(((GetField) exp).getVar1());
        } else if(exp instanceof ArrIndex){
            String name= getArrName((ArrIndex)exp);
            SymbolInfo info = variables.get(name);
            if (info == null) {
                throw new Exception("undefined variable \"" + ((Var) exp).getName() + "\". line: " + exp.getLine());
            }
            Type arrType = info.getType();
            while (arrType instanceof Array){
                arrType = ((Array)arrType).getType();
            }
            return arrType;
        } else {
            SymbolInfo info = variables.get(((Var) exp).getName());
            if (info == null) {
                throw new Exception("undefined variable \"" + ((Var) exp).getName() + "\". line: " + exp.getLine());
            }
            return info.getType();
        }
    }



    private List<LLVMValueRef> getArrayIndices(ArrIndex arrIndex) throws Exception{
        List<LLVMValueRef> list;
        if(arrIndex.getVar() instanceof ArrIndex){
            list = getArrayIndices((ArrIndex)arrIndex.getVar());
            list.add(genCode(arrIndex.getExp()));
        }else {
            list = new ArrayList<>();
            list.add(LLVMConstInt(LLVMInt32Type(), 0, 0));
            list.add(genCode(arrIndex.getExp()));
        }
        return list;
    }


    private Type getArrayType(Type type, List<Integer> lengths) {
        LLVMTypeRef llvmTypeRef = type.getLlvmtype();
        if (lengths != null) {
            for (int i = lengths.size() - 1; i >= 0; i--) {
                llvmTypeRef = LLVMArrayType(llvmTypeRef, lengths.get(i));
                type = new Array(lengths.get(i), type);
                type.setLlvmtype(llvmTypeRef);
            }
        }
        return type;
    }

    private Type getType(Specifier spec) throws Exception {
        Type retType;
        if (spec instanceof BasicType) {
            BasicType basicType = (BasicType) spec;
            LLVMTypeRef llvmRetType = getLLVMBasicType(basicType);
            Basic basic = new Basic(basicType.getType());
            basic.setLlvmtype(llvmRetType);
            retType = basic;
        } else {
            StructType structType = (StructType) spec;
            Struct struct = (Struct) structs.get(structType.getName()).getType();
            if (struct == null) {
                throw new Exception("type " + structType.getName() + " not defined. line: " + structType.getLine());
            }
            retType = struct;
        }
        return retType;
    }

    private LLVMTypeRef getLLVMBasicType(BasicType type) throws Exception {
        switch (type.getType()) {
            case ParserSym.INT:
                return LLVMInt32Type();
            case ParserSym.FLOAT:
                return LLVMFloatType();
            case ParserSym.CHAR:
                return LLVMInt8Type();
            default:
                throw new Exception("Unknown BasicType");
        }
    }

}
