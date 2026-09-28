package kz.company.notification.infrastructure.in.rest;

import jakarta.validation.ConstraintViolationException;
import kz.company.notification.domain.common.DomainException;
import kz.company.notification.domain.template.MissingTemplateVariablesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ошибки REST API в формате RFC 9457 Problem Details. Каждая ошибка содержит машинно-читаемый
 * {@code errorCode}; тип проблемы — {@code urn:problem-type:notification:<код>}.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    public static final String ERROR_CODE = "errorCode";

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException exception) {
        ProblemDetail problem = problem(ProblemStatus.forKind(exception.kind()), exception.errorCode(), exception.getMessage());
        if (exception instanceof MissingTemplateVariablesException missing) {
            problem.setProperty("missingVariables", missing.missingVariables());
        }
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleConcurrentModification(OptimisticLockingFailureException exception) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The resource was modified concurrently; retry the request");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request parameters are invalid");
        problem.setProperty("errors", exception.getConstraintViolations().stream()
                .map(violation -> Map.of("field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
                .toList());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        log.error("Unexpected error while processing request", exception);
        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Unexpected error; the request can be retried with the same Idempotency-Key");
        return ResponseEntity.internalServerError().body(problem);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> handleMissingHeader(MissingRequestHeaderException exception) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "REQUEST_INVALID",
                "Required header '" + exception.getHeaderName() + "' is not present");
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request body is invalid");
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage()))
                .toList();
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().headers(headers).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem && (problem.getProperties() == null
                || !problem.getProperties().containsKey(ERROR_CODE))) {
            problem.setProperty(ERROR_CODE, statusCode.is4xxClientError() ? "REQUEST_INVALID" : "INTERNAL_ERROR");
        }
        return super.handleExceptionInternal(exception, body, headers, statusCode, request);
    }

    public static ProblemDetail problem(HttpStatusCode status, String errorCode, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:problem-type:notification:" + errorCode.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setTitle(titleOf(errorCode));
        problem.setProperty(ERROR_CODE, errorCode);
        return problem;
    }

    private static String titleOf(String errorCode) {
        String words = errorCode.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
