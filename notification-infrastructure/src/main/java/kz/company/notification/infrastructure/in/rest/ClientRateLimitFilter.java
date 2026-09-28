package kz.company.notification.infrastructure.in.rest;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Ограничение частоты запросов на клиента (Resilience4j RateLimiter, конфигурация {@code clients}).
 * При превышении — 429 Too Many Requests с заголовком Retry-After (RFC 9110, п. 10.2.3).
 *
 * <p>Лимит считается в памяти экземпляра: при N экземплярах общий лимит клиента — N × limit.
 * Распределённый лимит (Redis/Bucket4j) — следующий шаг, см. docs/adr/0008-rate-limiting.md.
 */
public class ClientRateLimitFilter extends OncePerRequestFilter {

    private static final String CONFIGURATION = "clients";

    private final RateLimiterRegistry rateLimiters;
    private final JsonMapper jsonMapper;

    public ClientRateLimitFilter(RateLimiterRegistry rateLimiters, JsonMapper jsonMapper) {
        this.rateLimiters = rateLimiters;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<String> clientId = AuthenticatedClient.clientIdValue(authentication);
        if (clientId.isEmpty() || authentication == null || !authentication.isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }
        RateLimiter limiter = rateLimiters.rateLimiter("client-" + clientId.get(), CONFIGURATION);
        if (limiter.acquirePermission()) {
            chain.doFilter(request, response);
            return;
        }
        long retryAfterSeconds = Math.max(1, (long) Math.ceil(
                limiter.getRateLimiterConfig().getLimitRefreshPeriod().toMillis() / 1000.0));
        ProblemDetail problem = ApiExceptionHandler.problem(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                "Too many requests for client; retry after " + retryAfterSeconds + " seconds");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", problem.getType().toString());
        body.put("title", problem.getTitle());
        body.put("status", problem.getStatus());
        body.put("detail", problem.getDetail());
        body.put("instance", request.getRequestURI());
        body.put(ApiExceptionHandler.ERROR_CODE, "RATE_LIMIT_EXCEEDED");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
