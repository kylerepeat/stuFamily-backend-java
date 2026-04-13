package com.stufamily.backend.shared.security;

import com.stufamily.backend.identity.domain.repository.AdminUserRepository;
import com.stufamily.backend.identity.domain.repository.UserRepository;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysAdminUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysUserDO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
        JwtTokenProvider jwtTokenProvider,
        AdminUserRepository adminUserRepository,
        UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.adminUserRepository = adminUserRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);
                Long userId = Long.valueOf(claims.getSubject());
                long tokenVersion = extractLong(claims.get("tv"));
                AuthAudience audience = resolveAudience(claims.get("audience"));
                if (audience == null) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                if (!isTokenUserValid(userId, tokenVersion, audience)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                SecurityUser user = new SecurityUser(
                    userId,
                    Objects.toString(claims.get("username"), "unknown"),
                    "",
                    extractRoles(claims.get("roles"))
                );
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            return null;
        }
        return auth.substring(7);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Object rolesClaim) {
        if (rolesClaim instanceof List<?> values) {
            return values.stream().map(String::valueOf).toList();
        }
        return Collections.emptyList();
    }

    private long extractLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private long tokenVersion(SysUserDO userDO) {
        return userDO.getTokenVersion() == null ? 0L : userDO.getTokenVersion();
    }

    private long tokenVersion(SysAdminUserDO userDO) {
        return userDO.getTokenVersion() == null ? 0L : userDO.getTokenVersion();
    }

    private AuthAudience resolveAudience(Object audienceClaim) {
        if (!(audienceClaim instanceof String text) || !StringUtils.hasText(text)) {
            return null;
        }
        try {
            return AuthAudience.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isTokenUserValid(Long userId, long tokenVersion, AuthAudience audience) {
        if (audience == AuthAudience.ADMIN) {
            SysAdminUserDO userDO = adminUserRepository.findById(userId).orElse(null);
            return userDO != null
                && "ACTIVE".equalsIgnoreCase(userDO.getStatus())
                && tokenVersion(userDO) == tokenVersion;
        }
        if (audience == AuthAudience.WEIXIN) {
            SysUserDO userDO = userRepository.findById(userId).orElse(null);
            return userDO != null
                && "ACTIVE".equalsIgnoreCase(userDO.getStatus())
                && tokenVersion(userDO) == tokenVersion;
        }
        return false;
    }
}
