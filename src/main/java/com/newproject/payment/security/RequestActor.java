package com.newproject.payment.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RequestActor {
    public static final String CUSTOMER_ID_HEADER = "X-Authenticated-Customer-Id";

    public boolean isAuthenticated() {
        Authentication authentication = authentication();
        return authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
    }

    public boolean isAdmin() {
        Authentication authentication = authentication();
        return authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equalsIgnoreCase(authority.getAuthority()));
    }

    public Optional<Long> currentCustomerId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return Optional.empty();
        }
        String header = request.getHeader(CUSTOMER_ID_HEADER);
        if (!StringUtils.hasText(header)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(header.trim()));
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException("Invalid authenticated customer context");
        }
    }

    public Long resolveScopedCustomerId(Long requestedCustomerId) {
        if (!isAuthenticated() || isAdmin()) {
            return requestedCustomerId;
        }
        Long currentCustomerId = requireCurrentCustomerId();
        if (requestedCustomerId != null && !Objects.equals(requestedCustomerId, currentCustomerId)) {
            throw new AccessDeniedException("You cannot access another customer's payment instruments");
        }
        return currentCustomerId;
    }

    private Long requireCurrentCustomerId() {
        return currentCustomerId().orElseThrow(() -> new AccessDeniedException("Missing authenticated customer context"));
    }

    private Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
