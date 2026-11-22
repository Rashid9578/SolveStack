package com.solvestack.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(TopicNotFoundException.class)
    ProblemDetail handleTopicNotFound(TopicNotFoundException error) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
        problem.setTitle("Topic not found");
        return problem;
    }
}
