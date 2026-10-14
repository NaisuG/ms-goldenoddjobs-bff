package cl.goldenoddjobs.bff.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Valida el header x-api-secret.
 * - /api/notice: SIEMPRE lo exige (lo llama la Lambda B, sin usuario/JWT).
 * - resto de /api/** (menos /api/health): solo si bff.enforce-secret-on-all=true, para que el BFF
 *   únicamente acepte tráfico que pasó por API Gateway (que inyecta el header).
 */
public class ApiSecretFilter extends OncePerRequestFilter {

    public static final String HEADER = "x-api-secret";

    private final byte[] secret;
    private final boolean enforceOnAll;

    public ApiSecretFilter(String secret, boolean enforceOnAll) {
        this.secret = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        this.enforceOnAll = enforceOnAll;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean requiere = path.equals("/api/notice")
                || (enforceOnAll && path.startsWith("/api/") && !path.equals("/api/health"));

        // OPTIONS (preflight CORS) nunca lleva el header
        if (requiere && !"OPTIONS".equalsIgnoreCase(request.getMethod()) && !secretoValido(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"x-api-secret invalido o ausente\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean secretoValido(HttpServletRequest request) {
        String recibido = request.getHeader(HEADER);
        if (recibido == null || secret.length == 0) {
            return false;
        }
        return MessageDigest.isEqual(secret, recibido.getBytes(StandardCharsets.UTF_8)); // tiempo constante
    }
}
