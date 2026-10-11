package in.saisahayadri.academy.config;

import in.saisahayadri.academy.model.Role;
import in.saisahayadri.academy.model.UserAccount;
import in.saisahayadri.academy.repository.UserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final Environment environment;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdmin(Environment environment, UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.environment = environment;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = environment.getProperty("APP_BOOTSTRAP_ADMIN_USERNAME");
        String email = environment.getProperty("APP_BOOTSTRAP_ADMIN_EMAIL");
        String password = environment.getProperty("APP_BOOTSTRAP_ADMIN_PASSWORD");
        if (username == null && email == null && password == null) return;
        if (username == null || email == null || password == null || password.length() < 12) {
            throw new IllegalStateException("Set APP_BOOTSTRAP_ADMIN_USERNAME, APP_BOOTSTRAP_ADMIN_EMAIL, and a 12+ character APP_BOOTSTRAP_ADMIN_PASSWORD together.");
        }
        if (users.findByUsernameIgnoreCase(username).isPresent() || users.findByEmailIgnoreCase(email).isPresent()) return;
        users.save(new UserAccount(username, email, passwordEncoder.encode(password), Role.ADMIN));
    }
}
