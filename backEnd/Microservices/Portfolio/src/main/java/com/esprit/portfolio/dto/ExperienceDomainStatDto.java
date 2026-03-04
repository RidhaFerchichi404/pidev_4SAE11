package com.esprit.portfolio.dto;

import com.esprit.portfolio.entity.Domain;

/**
 * Projection DTO for admin dashboard: number of experiences per domain.
 * Used by the JPQL constructor expression in ExperienceRepository.
 */
public record ExperienceDomainStatDto(Domain domain, Long count) {}
