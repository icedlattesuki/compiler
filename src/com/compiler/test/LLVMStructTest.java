package com.compiler.test;

import org.bytedeco.javacpp.*;

import static org.bytedeco.javacpp.LLVM.*;

public class LLVMStructTest {
    public static void main(String[] args){
        // 初始化
        BytePointer error = new BytePointer((Pointer)null); // Used to retrieve messages from functions
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeDisassembler();
        LLVMInitializeNativeTarget();

        // 创建模块
        LLVMModuleRef mod = LLVMModuleCreateWithName("fac_module");
        LLVMContextRef context = LLVMGetModuleContext(mod);

        // 测试内容
        LLVMTypeRef s1 = LLVMStructCreateNamed(context,"s1");
        LLVMTypeRef[] typelist = {LLVMInt32Type(),LLVMInt32Type()};
        LLVMStructSetBody(s1,new PointerPointer(typelist),2,1);



        // 检查
        LLVMVerifyModule(mod, LLVMAbortProcessAction, error);
        LLVMDisposeMessage(error); // Handler == LLVMAbortProcessAction -> No need to check errors


        // 导出
        LLVMPassManagerRef pass = LLVMCreatePassManager();
        LLVMAddConstantPropagationPass(pass);
        LLVMAddInstructionCombiningPass(pass);
        LLVMAddPromoteMemoryToRegisterPass(pass);
        // LLVMAddDemoteMemoryToRegisterPass(pass); // Demotes every possible value to memory
        LLVMAddGVNPass(pass);
        LLVMAddCFGSimplificationPass(pass);
        LLVMRunPassManager(pass, mod);
        LLVMDumpModule(mod);


        LLVMDisposePassManager(pass);
//        LLVMDisposeBuilder(builder);
//        LLVMDisposeExecutionEngine(engine);
    }
}
