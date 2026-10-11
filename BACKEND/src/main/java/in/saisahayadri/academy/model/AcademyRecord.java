package in.saisahayadri.academy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "academy_records", indexes = {
        @Index(name = "idx_records_batch_type", columnList = "batch_id, type"),
        @Index(name = "idx_records_student_type", columnList = "student_id, type")
})
public class AcademyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecordType type;

    @NotBlank
    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 3000)
    private String details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private AcademyBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private UserAccount student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private StudyMaterial attachment;

    private LocalDate eventDate;

    @Column(length = 80)
    private String status;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AcademyRecord() {
    }

    public AcademyRecord(RecordType type, String title, String details, AcademyBatch batch,
                         UserAccount student, LocalDate eventDate, String status) {
        this.type = type;
        this.title = title;
        this.details = details;
        this.batch = batch;
        this.student = student;
        this.eventDate = eventDate;
        this.status = status;
    }

    public Long getId() { return id; }
    public RecordType getType() { return type; }
    public String getTitle() { return title; }
    public String getDetails() { return details; }
    public AcademyBatch getBatch() { return batch; }
    public UserAccount getStudent() { return student; }
    public StudyMaterial getAttachment() { return attachment; }
    public LocalDate getEventDate() { return eventDate; }
    public String getStatus() { return status; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setType(RecordType type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setDetails(String details) { this.details = details; }
    public void setBatch(AcademyBatch batch) { this.batch = batch; }
    public void setStudent(UserAccount student) { this.student = student; }
    public void setAttachment(StudyMaterial attachment) { this.attachment = attachment; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public void setStatus(String status) { this.status = status; }
    public void setActive(boolean active) { this.active = active; }
}
