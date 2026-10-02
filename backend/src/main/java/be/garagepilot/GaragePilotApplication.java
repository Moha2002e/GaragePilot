package be.garagepilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class GaragePilotApplication {
  public static void main(String[] args) {
    SpringApplication.run(GaragePilotApplication.class, args);
  }
}
