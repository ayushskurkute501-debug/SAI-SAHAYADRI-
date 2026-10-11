package in.saisahayadri.academy.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final SecurityContextRepository securityContextRepository;

    public RoleLoginSuccessHandler(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String expectedRole = request.getParameter("expectedRole");
        String role = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")) ? "ADMIN" : "STUDENT";
        if (expectedRole != null && !expectedRole.equals(role)) {
            SecurityContextHolder.clearContext();
            request.getSession(false).invalidate();
            response.sendRedirect(request.getContextPath() + "/login?error=role");
            return;
        }
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
        response.sendRedirect(request.getContextPath() + (role.equals("ADMIN") ? "/admin" : "/student"));
    }
}
