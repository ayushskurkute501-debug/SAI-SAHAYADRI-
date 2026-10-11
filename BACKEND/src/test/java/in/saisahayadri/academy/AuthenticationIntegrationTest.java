package in.saisahayadri.academy;

import in.saisahayadri.academy.model.Role;
import in.saisahayadri.academy.model.AcademyBatch;
import in.saisahayadri.academy.model.RecordType;
import in.saisahayadri.academy.model.StudentProfile;
import in.saisahayadri.academy.model.UserAccount;
import in.saisahayadri.academy.repository.AcademyRecordRepository;
import in.saisahayadri.academy.repository.BatchRepository;
import in.saisahayadri.academy.repository.StudentProfileRepository;
import in.saisahayadri.academy.repository.UserAccountRepository;
import in.saisahayadri.academy.service.AcademyRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationIntegrationTest {
    private static final String ADMIN_PASSWORD = "Admin-Password-2026!";
    private static final String STUDENT_PASSWORD = "Student-Password-2026!";

    @Autowired private MockMvc mvc;
    @Autowired private UserAccountRepository users;
    @Autowired private StudentProfileRepository profiles;
    @Autowired private BatchRepository batches;
    @Autowired private AcademyRecordRepository records;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AcademyRecordService recordService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        records.deleteAll();
        profiles.deleteAll();
        users.deleteAll();
        batches.deleteAll();
        users.save(new UserAccount("academy-admin", "admin@example.org",
                passwordEncoder.encode(ADMIN_PASSWORD), Role.ADMIN));
        UserAccount student = users.save(new UserAccount("student-one", "student@example.org",
                passwordEncoder.encode(STUDENT_PASSWORD), Role.STUDENT));
        profiles.save(new StudentProfile(users.getReferenceById(student.getId()), "Student One", "", null));
    }

    @Test
    void loginPageOffersOnlyAdminAndStudentOptions() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Prepare with purpose.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/login")));

        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Admin Login")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Student Login")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Trainer Login"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Faculty Login"))));
    }

    @Test
    void adminAndStudentSessionsAreConfinedToTheirDashboards() throws Exception {
        MvcResult adminLogin = mvc.perform(post("/login").with(csrf())
                        .param("username", "academy-admin")
                        .param("password", ADMIN_PASSWORD)
                        .param("expectedRole", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andReturn();
        MockHttpSession adminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);
        mvc.perform(get("/admin").session(adminSession)).andExpect(status().isOk());
        mvc.perform(get("/student").session(adminSession)).andExpect(status().isForbidden());

        MvcResult studentLogin = mvc.perform(post("/login").with(csrf())
                        .param("username", "student@example.org")
                        .param("password", STUDENT_PASSWORD)
                        .param("expectedRole", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student"))
                .andReturn();
        MockHttpSession studentSession = (MockHttpSession) studentLogin.getRequest().getSession(false);
        mvc.perform(get("/student").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Student Dashboard")));
        mvc.perform(get("/admin").session(studentSession)).andExpect(status().isForbidden());
    }

    @Test
    void rememberMePersistsInDatabaseAndRestoresLoginWithoutTheSession() throws Exception {
        MvcResult login = mvc.perform(post("/login").with(csrf())
                        .param("username", "academy-admin")
                        .param("password", ADMIN_PASSWORD)
                        .param("expectedRole", "ADMIN")
                        .param("remember-me", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andReturn();

        jakarta.servlet.http.Cookie rememberCookie = login.getResponse().getCookie("remember-me");
        assertThat(rememberCookie).isNotNull();
        assertThat(rememberCookie.getMaxAge()).isGreaterThan(0);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from persistent_logins where username = ?", Integer.class, "academy-admin"))
                .isEqualTo(1);

        MvcResult restored = mvc.perform(get("/admin").cookie(rememberCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Admin dashboard")))
                .andReturn();

        mvc.perform(post("/logout").session((MockHttpSession) restored.getRequest().getSession(false)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from persistent_logins where username = ?", Integer.class, "academy-admin"))
                .isZero();
    }

    @Test
    void roleSelectionMismatchAndInvalidCredentialsDoNotCreateAuthenticatedSessions() throws Exception {
        mvc.perform(post("/login").with(csrf())
                        .param("username", "academy-admin")
                        .param("password", ADMIN_PASSWORD)
                        .param("expectedRole", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=role"));

        mvc.perform(post("/login").with(csrf())
                        .param("username", "student-one")
                        .param("password", "wrong")
                        .param("expectedRole", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=credentials"));
    }

    @Test
    void adminCanChangeUsernameAndPasswordAndLogInWithTheSavedCredentials() throws Exception {
        MvcResult login = mvc.perform(post("/login").with(csrf())
                        .param("username", "academy-admin")
                        .param("password", ADMIN_PASSWORD)
                        .param("expectedRole", "ADMIN"))
                .andReturn();
        MockHttpSession adminSession = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(post("/admin/account/security").session(adminSession).with(csrf())
                        .param("currentPassword", ADMIN_PASSWORD)
                        .param("username", "academy-owner")
                        .param("newPassword", "Updated-Admin-Password-2026!")
                        .param("confirmPassword", "Updated-Admin-Password-2026!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin#account"));

        mvc.perform(get("/admin").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("academy-owner")));
        UserAccount updated = users.findByUsernameIgnoreCase("academy-owner").orElseThrow();
        assertThat(passwordEncoder.matches("Updated-Admin-Password-2026!", updated.getPasswordHash())).isTrue();

        mvc.perform(post("/logout").session(adminSession).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/login").with(csrf())
                        .param("username", "academy-admin")
                        .param("password", ADMIN_PASSWORD)
                        .param("expectedRole", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=credentials"));
        mvc.perform(post("/login").with(csrf())
                        .param("username", "academy-owner")
                        .param("password", "Updated-Admin-Password-2026!")
                        .param("expectedRole", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void adminCanCreateStudentWhoCanThenLogInAndLogOut() throws Exception {
        MvcResult adminLogin = mvc.perform(post("/login").with(csrf())
                        .param("username", "academy-admin")
                        .param("password", ADMIN_PASSWORD)
                        .param("expectedRole", "ADMIN"))
                .andReturn();
        MockHttpSession adminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);
        mvc.perform(post("/admin/students").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "new-student")
                        .param("email", "new@example.org")
                        .param("password", "New-Student-Secret-2026")
                        .param("fullName", "New Student")
                        .param("phone", "9999999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin#students"));

        UserAccount created = users.findByUsernameIgnoreCase("new-student").orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.STUDENT);
        assertThat(created.getPasswordHash()).isNotEqualTo("New-Student-Secret-2026");
        assertThat(passwordEncoder.matches("New-Student-Secret-2026", created.getPasswordHash())).isTrue();

        MvcResult studentLogin = mvc.perform(post("/login").with(csrf())
                        .param("username", "new-student")
                        .param("password", "New-Student-Secret-2026")
                        .param("expectedRole", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student"))
                .andReturn();
        mvc.perform(post("/logout").session((MockHttpSession) studentLogin.getRequest().getSession(false)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?logout"));
    }

    @Test
    void studentsCannotCreateStudentAccounts() throws Exception {
        MvcResult studentLogin = mvc.perform(post("/login").with(csrf())
                        .param("username", "student-one")
                        .param("password", STUDENT_PASSWORD)
                        .param("expectedRole", "STUDENT"))
                .andReturn();
        mvc.perform(post("/admin/students")
                        .session((MockHttpSession) studentLogin.getRequest().getSession(false))
                        .with(csrf())
                        .param("username", "intruder")
                        .param("email", "intruder@example.org")
                        .param("password", "another-strong-password")
                        .param("fullName", "Intruder"))
                .andExpect(status().isForbidden());
        assertThat(users.findByUsernameIgnoreCase("intruder")).isEmpty();
    }

    @Test
    void adminCanDeleteStudentWithoutRemovingHistoricalAccountData() throws Exception {
        UserAccount student = users.findByUsernameIgnoreCase("student-one").orElseThrow();
        MvcResult login = mvc.perform(post("/login").with(csrf())
                        .param("username", "academy-admin")
                        .param("password", ADMIN_PASSWORD)
                        .param("expectedRole", "ADMIN"))
                .andReturn();
        mvc.perform(post("/admin/students/{id}/delete", student.getId())
                        .session((MockHttpSession) login.getRequest().getSession(false))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin#students"));

        assertThat(users.findById(student.getId()).orElseThrow().isActive()).isFalse();
        mvc.perform(post("/login").with(csrf())
                        .param("username", "student-one")
                        .param("password", STUDENT_PASSWORD)
                        .param("expectedRole", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=credentials"));
    }

    @Test
    void adminCanAttachPdfToBatchUpdateAndOnlyBatchStudentsCanDownload() throws Exception {
        UserAccount student = users.findByUsernameIgnoreCase("student-one").orElseThrow();
        UserAccount other = users.save(new UserAccount("student-two", "student-two@example.org",
                passwordEncoder.encode(STUDENT_PASSWORD), Role.STUDENT));
        profiles.save(new StudentProfile(other, "Student Two", "", null));
        AcademyBatch sharedBatch = batches.save(new AcademyBatch("Morning Batch"));
        profiles.findByUserId(student.getId()).orElseThrow().setBatch(sharedBatch);

        MvcResult adminLogin = mvc.perform(post("/login").with(csrf())
                        .param("username", "academy-admin")
                        .param("password", ADMIN_PASSWORD)
                        .param("expectedRole", "ADMIN"))
                .andReturn();
        MockMultipartFile pdf = new MockMultipartFile("attachmentFile", "notice.pdf",
                MediaType.APPLICATION_PDF_VALUE, "%PDF-1.7 academy notice".getBytes());
        mvc.perform(multipart("/admin/records")
                        .file(pdf)
                        .session((MockHttpSession) adminLogin.getRequest().getSession(false))
                        .with(csrf())
                        .param("type", "ANNOUNCEMENT")
                        .param("title", "Batch notice")
                        .param("details", "Read the attached notice")
                        .param("batchId", sharedBatch.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin#records"));

        MvcResult studentLogin = mvc.perform(post("/login").with(csrf())
                        .param("username", "student-one")
                        .param("password", STUDENT_PASSWORD)
                        .param("expectedRole", "STUDENT"))
                .andReturn();
        MockHttpSession studentSession = (MockHttpSession) studentLogin.getRequest().getSession(false);
        mvc.perform(get("/student").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Download attachment: notice.pdf")));
        Long attachmentId = records.findByActiveTrueOrderByCreatedAtDesc().get(0).getAttachment().getId();
        mvc.perform(get("/student/materials/{id}/file", attachmentId).session(studentSession))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes("%PDF-1.7 academy notice".getBytes()));

        MvcResult otherLogin = mvc.perform(post("/login").with(csrf())
                        .param("username", "student-two")
                        .param("password", STUDENT_PASSWORD)
                        .param("expectedRole", "STUDENT"))
                .andReturn();
        mvc.perform(get("/student/materials/{id}/file", attachmentId)
                        .session((MockHttpSession) otherLogin.getRequest().getSession(false)))
                .andExpect(status().isNotFound());
    }

    @Test
    void studentDashboardOnlyShowsOwnRecordsAndBatchSharedRecords() throws Exception {
        UserAccount student = users.findByUsernameIgnoreCase("student-one").orElseThrow();
        UserAccount other = users.save(new UserAccount("student-two", "student-two@example.org",
                passwordEncoder.encode(STUDENT_PASSWORD), Role.STUDENT));
        profiles.save(new StudentProfile(other, "Student Two", "", null));
        AcademyBatch sharedBatch = batches.save(new AcademyBatch("Morning Batch"));
        profiles.findByUserId(student.getId()).orElseThrow().setBatch(sharedBatch);
        recordService.create(RecordType.FEE, "My fee balance", "Installment due", null,
                student.getId(), null, "DUE");
        recordService.create(RecordType.FEE, "Private fee record", "Not for another student", null,
                other.getId(), null, "PAID");
        recordService.create(RecordType.ANNOUNCEMENT, "Batch notice", "Bring notebooks", sharedBatch.getId(),
                null, null, "");

        MvcResult login = mvc.perform(post("/login").with(csrf())
                        .param("username", "student-one")
                        .param("password", STUDENT_PASSWORD)
                        .param("expectedRole", "STUDENT"))
                .andReturn();
        mvc.perform(get("/student").session((MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("My fee balance")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Batch notice")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Private fee record"))));
    }
}
