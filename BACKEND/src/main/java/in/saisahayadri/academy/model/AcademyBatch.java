package in.saisahayadri.academy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "batches", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class AcademyBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    protected AcademyBatch() {
    }

    public AcademyBatch(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public void setName(String name) { this.name = name; }
    public void setActive(boolean active) { this.active = active; }
}
