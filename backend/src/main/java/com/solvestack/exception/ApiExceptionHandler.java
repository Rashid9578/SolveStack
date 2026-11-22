package com.solvestack.exception;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {
    @ExceptionHandler(TopicNotFoundException.class)
    ProblemDetail handleTopicNotFound(TopicNotFoundException error) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
        problem.setTitle("Topic not found");
        return problem;
    }

    @ExceptionHandler(AccountConflictException.class)
    ProblemDetail handleAccountConflict(AccountConflictException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail handleBadCredentials(BadCredentialsException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, error.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidFields(MethodArgumentNotValidException error) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Check the submitted fields.");
        var errors = error.getBindingResult().getFieldErrors().stream().collect(Collectors.groupingBy(
                FieldError::getField, LinkedHashMap::new,
                Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())));
        problem.setProperty("errors", errors);
        return problem;
    }
}
