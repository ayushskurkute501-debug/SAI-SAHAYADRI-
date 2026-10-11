package in.saisahayadri.academy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {
    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(length = 30)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private AcademyBatch batch;

    protected StudentProfile() {
    }

    public StudentProfile(UserAccount user, String fullName, String phone, AcademyBatch batch) {
        this.user = user;
        this.fullName = fullName;
        this.phone = phone;
        this.batch = batch;
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public AcademyBatch getBatch() { return batch; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setBatch(AcademyBatch batch) { this.batch = batch; }
}
