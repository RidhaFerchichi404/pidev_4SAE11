package com.esprit.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ProfileViewItem {
    private Long viewerId;
    private LocalDateTime viewedAt;
}
