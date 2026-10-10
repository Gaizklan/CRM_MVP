package org.example.crm.common.infrastructure.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Repositorios Spring Data de todos los módulos con la base {@link TenantAwareJpaRepository}. */
@Configuration
@EnableJpaRepositories(
    basePackages = "org.example.crm",
    repositoryBaseClass = TenantAwareJpaRepository.class)
public class PersistenceConfiguration {}
