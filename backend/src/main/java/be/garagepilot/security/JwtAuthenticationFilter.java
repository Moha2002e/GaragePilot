package be.garagepilot.security;

import be.garagepilot.model.User;
import be.garagepilot.repository.UserRepository;
import be.garagepilot.service.JwtService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final UserRepository users;

  public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
    this.jwtService = jwtService;
    this.users = users;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      jakarta.servlet.http.HttpServletResponse response,
      FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      chain.doFilter(request, response);
      return;
    }
    try {
      User user =
          users
              .findSecurityUserById(
                  java.util.UUID.fromString(jwtService.extractUserId(header.substring(7))))
              .filter(candidate -> candidate.isActive() && candidate.getGarage().isActive())
              .orElseThrow();
      CurrentUser principal =
          new CurrentUser(user.getId(), user.getGarage().getId(), user.getRole());
      var authentication =
          new UsernamePasswordAuthenticationToken(
              principal,
              null,
              java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (Exception ignored) {
      SecurityContextHolder.clearContext();
    }
    chain.doFilter(request, response);
  }
}
