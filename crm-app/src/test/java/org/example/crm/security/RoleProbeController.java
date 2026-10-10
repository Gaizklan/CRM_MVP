package org.example.crm.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Solo tests: endpoints protegidos por rol funcional para verificar la autorización. */
@RestController
class RoleProbeController {

  @GetMapping("/api/test/producer")
  @PreAuthorize("hasRole('PRODUCER')")
  String producer() {
    return "ok";
  }

  @GetMapping("/api/test/reviewer")
  @PreAuthorize("hasRole('CONTENT_REVIEWER')")
  String reviewer() {
    return "ok";
  }
}
