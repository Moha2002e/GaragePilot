package be.garagepilot.config;

import be.garagepilot.model.UserRole;
import be.garagepilot.repository.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlatformAdminBootstrap implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformAdminBootstrap.class);

  private final UserRepository users;
  private final String platformAdminEmail;

  public PlatformAdminBootstrap(
      UserRepository users,
      @Value("${app.platform-admin-email:${GARAGEPILOT_PLATFORM_ADMIN_EMAIL:}}")
          String platformAdminEmail) {
    this.users = users;
    this.platformAdminEmail = platformAdminEmail;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (platformAdminEmail == null || platformAdminEmail.isBlank()) {
      return;
    }
    String email = platformAdminEmail.trim().toLowerCase(Locale.ROOT);
    users
        .findByEmailIgnoreCase(email)
        .ifPresentOrElse(
            user -> {
              if (user.getRole() != UserRole.SUPER_ADMIN) {
                user.setRole(UserRole.SUPER_ADMIN);
                LOGGER.info("Configured the requested platform administrator account");
              }
            },
            () -> LOGGER.warn("The configured platform administrator account does not exist"));
  }
}
