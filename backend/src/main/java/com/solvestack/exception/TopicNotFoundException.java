package com.solvestack.exception;

public class TopicNotFoundException extends RuntimeException {
    public TopicNotFoundException() {
        super("The requested topic does not exist.");
    }
}
