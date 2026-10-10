package org.example.crm.admin.application;

import java.util.Set;
import org.example.crm.admin.domain.FunctionalRole;
import org.example.crm.admin.domain.SecurityLevel;
import org.example.crm.common.domain.BrandId;

/**
 * Datos de alta o modificación de un usuario interno. {@code password} es opcional al modificar.
 */
public record InternalUserCommand(
    String username,
    String password,
    SecurityLevel level,
    BrandId brand,
    Set<FunctionalRole> roles,
    Boolean enabled) {}
