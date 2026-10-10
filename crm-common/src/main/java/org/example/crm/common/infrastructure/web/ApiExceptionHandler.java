package org.example.crm.common.infrastructure.web;

import org.example.crm.common.application.tenant.TenantNotSetException;
import org.example.crm.common.domain.ConflictException;
import org.example.crm.common.domain.DomainException;
import org.example.crm.common.domain.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traducción de errores de dominio a respuestas RFC 7807 (Problem Details). */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  ProblemDetail notFound(NotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  ProblemDetail conflict(ConflictException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }

  @ExceptionHandler(TenantNotSetException.class)
  ProblemDetail tenantRequired(TenantNotSetException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(DomainException.class)
  ProblemDetail invalid(DomainException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }
}
