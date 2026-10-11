package in.saisahayadri.academy.repository;

import in.saisahayadri.academy.model.StudyMaterial;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    @EntityGraph(attributePaths = "batch")
    List<StudyMaterial> findByActiveTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "batch")
    List<StudyMaterial> findByBatchIdAndActiveTrueOrderByCreatedAtDesc(Long batchId);

    @EntityGraph(attributePaths = "batch")
    Optional<StudyMaterial> findByIdAndActiveTrue(Long id);
}
