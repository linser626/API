package com.airelay.security;

import com.airelay.common.ErrorCode;
import com.airelay.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Object details = authentication.getDetails();
        if (details instanceof JwtAuthFilter.JwtAuthenticationDetails jwtDetails) {
            return jwtDetails.userId();
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Object details = authentication.getDetails();
        if (details instanceof JwtAuthFilter.JwtAuthenticationDetails jwtDetails) {
            return jwtDetails.username();
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
