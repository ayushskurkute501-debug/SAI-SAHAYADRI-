package in.saisahayadri.academy.service;

import in.saisahayadri.academy.model.*;
import in.saisahayadri.academy.repository.BatchRepository;
import in.saisahayadri.academy.repository.StudentProfileRepository;
import in.saisahayadri.academy.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AccountService {
    private final UserAccountRepository users;
    private final StudentProfileRepository profiles;
    private final BatchRepository batches;
    private final PasswordEncoder passwordEncoder;
    private final PersistentTokenRepository persistentTokenRepository;

    public AccountService(UserAccountRepository users, StudentProfileRepository profiles,
                         BatchRepository batches, PasswordEncoder passwordEncoder,
                         PersistentTokenRepository persistentTokenRepository) {
        this.users = users;
        this.profiles = profiles;
        this.batches = batches;
        this.passwordEncoder = passwordEncoder;
        this.persistentTokenRepository = persistentTokenRepository;
    }

    @Transactional
    public UserAccount createStudent(String username, String email, String password, String fullName,
                                     String phone, Long batchId) {
        username = username.trim();
        email = email.trim().toLowerCase();
        if (users.findByUsernameIgnoreCase(username).isPresent() || users.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("That username or email is already in use.");
        }
        AcademyBatch batch = batchId == null ? null : batches.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Selected batch was not found."));
        UserAccount user = users.save(new UserAccount(username, email, passwordEncoder.encode(password), Role.STUDENT));
        profiles.save(new StudentProfile(user, fullName.trim(), phone == null ? "" : phone.trim(), batch));
        return user;
    }

    @Transactional
    public void updateStudent(Long id, String username, String email, String fullName, String phone, Long batchId) {
        UserAccount user = users.findById(id).filter(account -> account.getRole() == Role.STUDENT)
                .orElseThrow(() -> new IllegalArgumentException("Student account was not found."));
        String cleanUsername = username.trim();
        String cleanEmail = email.trim().toLowerCase();
        if (users.findByUsernameIgnoreCase(cleanUsername).filter(other -> !other.getId().equals(id)).isPresent()
                || users.findByEmailIgnoreCase(cleanEmail).filter(other -> !other.getId().equals(id)).isPresent()) {
            throw new IllegalArgumentException("That username or email is already in use.");
        }
        StudentProfile profile = profiles.findByUserId(id)
                .orElseThrow(() -> new IllegalArgumentException("Student profile was not found."));
        AcademyBatch batch = batchId == null ? null : batches.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Selected batch was not found."));
        user.setUsername(cleanUsername);
        user.setEmail(cleanEmail);
        profile.setFullName(fullName.trim());
        profile.setPhone(phone == null ? "" : phone.trim());
        profile.setBatch(batch);
    }

    @Transactional
    public UserAccount updateAdminCredentials(Long adminId, String currentPassword, String username,
                                              String newPassword, String confirmPassword) {
        UserAccount admin = users.findById(adminId).filter(account -> account.getRole() == Role.ADMIN && account.isActive())
                .orElseThrow(() -> new IllegalArgumentException("Administrator account was not found or is inactive."));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
            throw new IllegalArgumentException("The current password is incorrect.");
        }
        String cleanUsername = username == null ? "" : username.trim();
        if (cleanUsername.length() < 3 || cleanUsername.length() > 80) {
            throw new IllegalArgumentException("Username must be between 3 and 80 characters.");
        }
        boolean loginNameTaken = users.findByUsernameIgnoreCase(cleanUsername)
                .filter(other -> !other.getId().equals(adminId)).isPresent()
                || users.findByEmailIgnoreCase(cleanUsername)
                .filter(other -> !other.getId().equals(adminId)).isPresent();
        if (loginNameTaken) {
            throw new IllegalArgumentException("That username is already in use.");
        }
        if (StringUtils.hasText(newPassword)) {
            if (newPassword.length() < 12 || newPassword.length() > 72) {
                throw new IllegalArgumentException("New password must be between 12 and 72 characters.");
            }
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("New password and confirmation do not match.");
            }
            admin.setPasswordHash(passwordEncoder.encode(newPassword));
        } else if (StringUtils.hasText(confirmPassword)) {
            throw new IllegalArgumentException("Enter a new password or clear the confirmation field.");
        }
        persistentTokenRepository.removeUserTokens(admin.getUsername());
        admin.setUsername(cleanUsername);
        return admin;
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        UserAccount user = users.findById(id).filter(account -> account.getRole() == Role.STUDENT)
                .orElseThrow(() -> new IllegalArgumentException("Student account was not found."));
        if (user.isActive() && !active) {
            persistentTokenRepository.removeUserTokens(user.getUsername());
        }
        user.setActive(active);
    }

    @Transactional
    public AcademyBatch createBatch(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Batch name is required.");
        return batches.save(new AcademyBatch(trimmed));
    }

    public List<UserAccount> students() {
        return users.findByRoleOrderByUsername(Role.STUDENT);
    }
}
