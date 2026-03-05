package com.esprit.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileViewStats {
    private long totalViews;
    private long thisWeekViews;
    private long lastWeekViews;
}
