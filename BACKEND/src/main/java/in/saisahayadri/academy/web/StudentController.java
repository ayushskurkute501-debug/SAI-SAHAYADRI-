package in.saisahayadri.academy.web;

import in.saisahayadri.academy.model.*;
import in.saisahayadri.academy.repository.AcademyRecordRepository;
import in.saisahayadri.academy.repository.StudentProfileRepository;
import in.saisahayadri.academy.repository.UserAccountRepository;
import in.saisahayadri.academy.service.AcademyRecordService;
import in.saisahayadri.academy.service.StudyMaterialService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {
    private final UserAccountRepository users;
    private final StudentProfileRepository profiles;
    private final AcademyRecordService recordService;
    private final AcademyRecordRepository records;
    private final StudyMaterialService materialService;

    public StudentController(UserAccountRepository users, StudentProfileRepository profiles,
                             AcademyRecordService recordService, AcademyRecordRepository records,
                             StudyMaterialService materialService) {
        this.users = users;
        this.profiles = profiles;
        this.recordService = recordService;
        this.records = records;
        this.materialService = materialService;
    }

    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        UserAccount user = users.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated student account was not found."));
        StudentProfile profile = profiles.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Student profile was not found."));
        Long batchId = profile.getBatch() == null ? null : profile.getBatch().getId();
        List<AcademyRecord> shared = recordService.studentRecords(user.getId(), batchId);
        long lectureCount = records.findByStudentIdAndTypeAndActiveTrueOrderByCreatedAtDesc(user.getId(), RecordType.LECTURE_ATTENDANCE).size();
        long groundCount = records.findByStudentIdAndTypeAndActiveTrueOrderByCreatedAtDesc(user.getId(), RecordType.GROUND_ATTENDANCE).size();
        model.addAttribute("account", user);
        model.addAttribute("profile", profile);
        model.addAttribute("records", shared);
        model.addAttribute("materials", materialService.forStudent(batchId));
        model.addAttribute("lecturePercent", attendancePercent(user.getId(), RecordType.LECTURE_ATTENDANCE));
        model.addAttribute("groundPercent", attendancePercent(user.getId(), RecordType.GROUND_ATTENDANCE));
        model.addAttribute("lectureCount", lectureCount);
        model.addAttribute("groundCount", groundCount);
        return "student";
    }

    @GetMapping("/materials/{id}/file")
    @ResponseBody
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long id, Authentication authentication) {
        UserAccount user = users.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        StudentProfile profile = profiles.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        Long batchId = profile.getBatch() == null ? null : profile.getBatch().getId();
        var material = materialService.findActive(id)
                .filter(item -> item.getBatch() == null || (batchId != null && item.getBatch().getId().equals(batchId)))
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        var path = materialService.fileForStudent(id, batchId)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(material.getOriginalFilename(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(path));
    }

    private int attendancePercent(Long studentId, RecordType type) {
        List<AcademyRecord> attendance = records.findByStudentIdAndTypeAndActiveTrueOrderByCreatedAtDesc(studentId, type);
        if (attendance.isEmpty()) return 0;
        long present = attendance.stream().filter(record -> "PRESENT".equalsIgnoreCase(record.getStatus())).count();
        return (int) Math.round((present * 100.0) / attendance.size());
    }
}
