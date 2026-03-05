package com.esprit.portfolio.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfileViewRequest {
    /** The freelancer whose profile is being viewed. */
    private Long profileUserId;
    /** The logged-in viewer. Null if the visitor is anonymous / not logged in. */
    private Long viewerId;
}
