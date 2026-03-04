package com.esprit.portfolio.dto;

import com.esprit.portfolio.entity.Domain;

/**
 * Projection DTO for admin dashboard: number of skills per domain.
 * Used by the JPQL constructor expression in SkillRepository.
 */
public record SkillDomainStatDto(Domain domain, Long count) {}
