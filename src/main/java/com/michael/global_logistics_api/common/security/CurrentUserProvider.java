package com.michael.global_logistics_api.common.security;

import com.michael.global_logistics_api.common.domain.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public CurrentUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getDetails() instanceof CurrentUser currentUser)) {
            throw new UnauthorizedException("Authentication required");
        }

        return currentUser;
    }
}
