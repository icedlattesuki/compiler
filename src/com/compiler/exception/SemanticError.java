package com.compiler.exception;

public class SemanticError extends RuntimeException {
    public SemanticError(String message) {
        super(message);
    }
}
