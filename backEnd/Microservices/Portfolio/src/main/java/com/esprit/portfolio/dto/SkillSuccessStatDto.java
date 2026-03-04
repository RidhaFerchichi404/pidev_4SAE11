package com.esprit.portfolio.dto;

/**
 * Admin stat: for a given skill, how many evaluation attempts were made
 * and what percentage of them passed.
 */
public record SkillSuccessStatDto(String skillName, Long totalAttempts, Long passedCount, Double successRate) {}
