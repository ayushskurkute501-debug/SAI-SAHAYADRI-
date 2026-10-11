package in.saisahayadri.academy.service;

import in.saisahayadri.academy.model.*;
import in.saisahayadri.academy.repository.AcademyRecordRepository;
import in.saisahayadri.academy.repository.BatchRepository;
import in.saisahayadri.academy.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AcademyRecordService {
    private final AcademyRecordRepository records;
    private final BatchRepository batches;
    private final UserAccountRepository users;

    public AcademyRecordService(AcademyRecordRepository records, BatchRepository batches, UserAccountRepository users) {
        this.records = records;
        this.batches = batches;
        this.users = users;
    }

    @Transactional
    public AcademyRecord create(RecordType type, String title, String details, Long batchId, Long studentId,
                               LocalDate eventDate, String status) {
        return create(type, title, details, batchId, studentId, eventDate, status, null);
    }

    @Transactional
    public AcademyRecord create(RecordType type, String title, String details, Long batchId, Long studentId,
                                LocalDate eventDate, String status, StudyMaterial attachment) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("A title is required.");
        if (type == null) throw new IllegalArgumentException("Choose a record category.");
        if (attachment != null && (studentId != null || type == RecordType.LECTURE_ATTENDANCE
                || type == RecordType.GROUND_ATTENDANCE || type == RecordType.FEE
                || type == RecordType.PAYMENT || type == RecordType.MOCK_RESULT
                || type == RecordType.PHYSICAL_PROGRESS)) {
            throw new IllegalArgumentException("Attachments can only be added to academy-wide or batch-shared updates.");
        }
        boolean studentOnly = switch (type) {
            case LECTURE_ATTENDANCE, GROUND_ATTENDANCE, FEE, PAYMENT, MOCK_RESULT, PHYSICAL_PROGRESS -> true;
            default -> false;
        };
        if (studentOnly && studentId == null) {
            throw new IllegalArgumentException("Select the student for this record.");
        }
        if (studentOnly && batchId != null) {
            throw new IllegalArgumentException("Personal records cannot be assigned to a batch.");
        }
        if (studentId != null && batchId != null) {
            throw new IllegalArgumentException("A record can target either a student or a batch, not both.");
        }
        if (!studentOnly && type != RecordType.NOTIFICATION && studentId != null) {
            throw new IllegalArgumentException("This record must be shared with a batch or all students.");
        }
        AcademyBatch batch = batchId == null ? null : batches.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Selected batch was not found."));
        UserAccount student = studentId == null ? null : users.findById(studentId)
                .filter(user -> user.getRole() == Role.STUDENT)
                .orElseThrow(() -> new IllegalArgumentException("Selected student was not found."));
        AcademyRecord record = new AcademyRecord(type, title.trim(), details == null ? "" : details.trim(),
                batch, student, eventDate, status == null ? "" : status.trim());
        record.setAttachment(attachment);
        return records.save(record);
    }

    @Transactional
    public void deactivate(Long recordId) {
        AcademyRecord record = records.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Record was not found."));
        record.setActive(false);
    }

    public List<AcademyRecord> studentRecords(Long studentId, Long batchId) {
        List<AcademyRecord> visible = new java.util.ArrayList<>(records.findByStudentIdAndActiveTrueOrderByCreatedAtDesc(studentId));
        if (batchId != null) {
            visible.addAll(records.findByBatchIdAndStudentIsNullAndActiveTrueOrderByCreatedAtDesc(batchId));
        }
        visible.addAll(records.findByActiveTrueAndBatchIsNullAndStudentIsNullOrderByCreatedAtDesc());
        return visible.stream().distinct()
                .sorted(java.util.Comparator.comparing(AcademyRecord::getCreatedAt).reversed())
                .toList();
    }

    public List<AcademyRecord> adminRecords() {
        return records.findByActiveTrueOrderByCreatedAtDesc();
    }

    public List<AcademyRecord> studentRecordsOfType(Long studentId, RecordType type) {
        return records.findByStudentIdAndTypeAndActiveTrueOrderByCreatedAtDesc(studentId, type);
    }
}
