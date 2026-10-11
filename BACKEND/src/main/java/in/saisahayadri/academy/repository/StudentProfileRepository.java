package in.saisahayadri.academy.repository;

import in.saisahayadri.academy.model.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    @EntityGraph(attributePaths = {"user", "batch"})
    Optional<StudentProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "batch"})
    List<StudentProfile> findAllByOrderByFullName();
}
