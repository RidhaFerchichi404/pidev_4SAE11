package com.esprit.portfolio.dto;

/**
 * Admin stat: how many distinct freelancers have a given skill,
 * plus the percentage share out of all freelancers who have any skill.
 */
public record SkillUsageStatDto(String skillName, Long userCount, Double percentage) {}
