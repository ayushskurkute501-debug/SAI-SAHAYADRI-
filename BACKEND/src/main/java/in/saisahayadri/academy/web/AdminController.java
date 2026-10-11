package in.saisahayadri.academy.web;

import in.saisahayadri.academy.model.*;
import in.saisahayadri.academy.repository.AcademyRecordRepository;
import in.saisahayadri.academy.repository.BatchRepository;
import in.saisahayadri.academy.repository.StudentProfileRepository;
import in.saisahayadri.academy.repository.UserAccountRepository;
import in.saisahayadri.academy.service.AccountService;
import in.saisahayadri.academy.service.AcademyRecordService;
import in.saisahayadri.academy.service.StudyMaterialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AccountService accounts;
    private final AcademyRecordService recordService;
    private final AcademyRecordRepository records;
    private final BatchRepository batches;
    private final StudentProfileRepository profiles;
    private final UserAccountRepository users;
    private final StudyMaterialService materialService;
    private final SecurityContextRepository securityContextRepository;

    public AdminController(AccountService accounts, AcademyRecordService recordService,
                           AcademyRecordRepository records, BatchRepository batches,
                           StudentProfileRepository profiles, UserAccountRepository users,
                           StudyMaterialService materialService,
                           SecurityContextRepository securityContextRepository) {
        this.accounts = accounts;
        this.recordService = recordService;
        this.records = records;
        this.batches = batches;
        this.profiles = profiles;
        this.users = users;
        this.materialService = materialService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("students", accounts.students());
        model.addAttribute("profileById", profiles.findAllByOrderByFullName().stream()
                .collect(java.util.stream.Collectors.toMap(StudentProfile::getId, profile -> profile)));
        model.addAttribute("batches", batches.findByActiveTrueOrderByName());
        model.addAttribute("records", recordService.adminRecords());
        model.addAttribute("materials", materialService.forAdmin());
        model.addAttribute("recordTypes", RecordType.values());
        model.addAttribute("adminUsername", authentication.getName());
        model.addAttribute("recordForm", new RecordForm());
        model.addAttribute("studentForm", new StudentForm());
        return "admin";
    }

    @PostMapping("/account/security")
    public String updateAdminCredentials(@ModelAttribute AdminSecurityForm form,
                                         Authentication authentication,
                                         HttpServletRequest request,
                                         HttpServletResponse response,
                                         RedirectAttributes redirect) {
        UserAccount admin = users.findByUsernameIgnoreCase(authentication.getName())
                .filter(account -> account.getRole() == Role.ADMIN)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN));
        try {
            UserAccount updated = accounts.updateAdminCredentials(admin.getId(), form.currentPassword,
                    form.username, form.newPassword, form.confirmPassword);
            var updatedAuthentication = new UsernamePasswordAuthenticationToken(
                    updated.getUsername(), null, authentication.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(updatedAuthentication);
            securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
            redirect.addFlashAttribute("successMessage", "Admin login details updated. Use the new username and password next time.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#account";
    }

    @PostMapping("/students")
    public String createStudent(@ModelAttribute("studentForm") StudentForm form,
                                BindingResult binding, Model model, RedirectAttributes redirect) {
        validateStudent(form, binding, true);
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Check the student fields. Use a password of at least 12 characters.");
            return "redirect:/admin#students";
        }
        try {
            accounts.createStudent(form.username, form.email, form.password, form.fullName, form.phone, form.batchId);
            redirect.addFlashAttribute("successMessage", "Student account created. The student can log in now.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#students";
    }

    @PostMapping("/students/{id}/edit")
    public String editStudent(@PathVariable Long id, @ModelAttribute StudentForm form,
                              RedirectAttributes redirect) {
        try {
            if (form.username == null || form.email == null || form.fullName == null
                    || !form.email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new IllegalArgumentException("Enter a valid name, username and email.");
            }
            accounts.updateStudent(id, form.username, form.email, form.fullName, form.phone, form.batchId);
            redirect.addFlashAttribute("successMessage", "Student profile updated.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#students";
    }

    @PostMapping({"/students/{id}/deactivate", "/students/{id}/delete"})
    public String deactivateStudent(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            accounts.setActive(id, false);
            redirect.addFlashAttribute("successMessage", "Student account deleted. Historical records have been retained.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#students";
    }

    @PostMapping("/students/{id}/activate")
    public String activateStudent(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            accounts.setActive(id, true);
            redirect.addFlashAttribute("successMessage", "Student account activated.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#students";
    }

    @PostMapping("/batches")
    public String createBatch(@RequestParam @NotBlank String name, RedirectAttributes redirect) {
        try {
            accounts.createBatch(name);
            redirect.addFlashAttribute("successMessage", "Batch created.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#batches";
    }

    @PostMapping("/records")
    public String createRecord(@ModelAttribute RecordForm form,
                               @RequestParam(value = "attachmentFile", required = false) MultipartFile attachmentFile,
                               RedirectAttributes redirect) {
        try {
            StudyMaterial attachment = null;
            if (attachmentFile != null && !attachmentFile.isEmpty()) {
                attachment = materialService.uploadUpdateAttachment(form.title, form.details, form.batchId, attachmentFile);
            }
            recordService.create(form.type, form.title, form.details, form.batchId, form.studentId,
                    form.eventDate, form.status, attachment);
            redirect.addFlashAttribute("successMessage", "Academy record published.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        } catch (IllegalStateException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        } catch (NullPointerException exception) {
            redirect.addFlashAttribute("errorMessage", "Complete the required record fields.");
        }
        return "redirect:/admin#records";
    }

    @PostMapping("/records/{id}/deactivate")
    public String deactivateRecord(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            recordService.deactivate(id);
            redirect.addFlashAttribute("successMessage", "Record removed from student view.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#records";
    }

    @PostMapping("/materials")
    public String uploadMaterial(@RequestParam String title, @RequestParam(required = false) String description,
                                 @RequestParam(required = false) Long batchId,
                                 @RequestParam("file") MultipartFile file, RedirectAttributes redirect) {
        try {
            materialService.upload(title, description, batchId, file);
            redirect.addFlashAttribute("successMessage", "Study material uploaded.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#materials";
    }

    @PostMapping("/materials/{id}/deactivate")
    public String deactivateMaterial(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            materialService.deactivate(id);
            redirect.addFlashAttribute("successMessage", "Study material removed from student access.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#materials";
    }

    @GetMapping("/materials/{id}/file")
    @ResponseBody
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long id) {
        var material = materialService.findActive(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        var path = materialService.fileForAdmin(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(material.getOriginalFilename(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(path));
    }

    @GetMapping("/reports.csv")
    @ResponseBody
    public org.springframework.http.ResponseEntity<String> exportReport() {
        StringBuilder csv = new StringBuilder("username,email,full_name,batch,active\n");
        for (UserAccount student : accounts.students()) {
            StudentProfile profile = profiles.findByUserId(student.getId()).orElse(null);
            csv.append(csv(student.getUsername())).append(',')
                    .append(csv(student.getEmail())).append(',')
                    .append(csv(profile == null ? "" : profile.getFullName())).append(',')
                    .append(csv(profile == null || profile.getBatch() == null ? "" : profile.getBatch().getName())).append(',')
                    .append(student.isActive()).append('\n');
        }
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=academy-students.csv")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    private String csv(String value) {
        String safe = value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private void validateStudent(StudentForm form, BindingResult binding, boolean requirePassword) {
        if (form.username == null || form.username.isBlank()) binding.rejectValue("username", "required");
        if (form.email == null || !form.email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) binding.rejectValue("email", "invalid");
        if (form.fullName == null || form.fullName.isBlank()) binding.rejectValue("fullName", "required");
        if (requirePassword && (form.password == null || form.password.length() < 12)) binding.rejectValue("password", "short");
    }

    public static class StudentForm {
        @NotBlank @Size(max = 80) private String username;
        @NotBlank @Email @Size(max = 180) private String email;
        @Size(min = 12, max = 72) private String password;
        @NotBlank @Size(max = 120) private String fullName;
        @Size(max = 30) private String phone;
        private Long batchId;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }
    }

    public static class RecordForm {
        private RecordType type;
        @NotBlank @Size(max = 160) private String title;
        @Size(max = 3000) private String details;
        private Long batchId;
        private Long studentId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate eventDate;
        @Size(max = 80) private String status;

        public RecordType getType() { return type; }
        public void setType(RecordType type) { this.type = type; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }
        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public LocalDate getEventDate() { return eventDate; }
        public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class AdminSecurityForm {
        private String currentPassword;
        private String username;
        private String newPassword;
        private String confirmPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}
