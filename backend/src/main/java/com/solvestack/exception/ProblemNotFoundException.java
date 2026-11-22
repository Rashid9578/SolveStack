package com.solvestack.exception;

public class ProblemNotFoundException extends RuntimeException {
    public ProblemNotFoundException() {
        super("The requested problem does not exist.");
    }
}
