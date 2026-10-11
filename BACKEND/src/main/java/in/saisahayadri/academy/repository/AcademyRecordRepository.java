package in.saisahayadri.academy.repository;

import in.saisahayadri.academy.model.AcademyRecord;
import in.saisahayadri.academy.model.RecordType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademyRecordRepository extends JpaRepository<AcademyRecord, Long> {
    @EntityGraph(attributePaths = {"batch", "student", "attachment"})
    List<AcademyRecord> findByActiveTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"batch", "student", "attachment"})
    List<AcademyRecord> findByActiveTrueAndBatchIsNullAndStudentIsNullOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"batch", "student", "attachment"})
    List<AcademyRecord> findByStudentIdAndActiveTrueOrderByCreatedAtDesc(Long studentId);

    @EntityGraph(attributePaths = {"batch", "student", "attachment"})
    List<AcademyRecord> findByBatchIdAndActiveTrueOrderByCreatedAtDesc(Long batchId);

    @EntityGraph(attributePaths = {"batch", "student", "attachment"})
    List<AcademyRecord> findByBatchIdAndStudentIsNullAndActiveTrueOrderByCreatedAtDesc(Long batchId);

    @EntityGraph(attributePaths = {"batch", "student", "attachment"})
    List<AcademyRecord> findByTypeAndActiveTrueOrderByCreatedAtDesc(RecordType type);

    @EntityGraph(attributePaths = {"batch", "student", "attachment"})
    List<AcademyRecord> findByStudentIdAndTypeAndActiveTrueOrderByCreatedAtDesc(Long studentId, RecordType type);

    @EntityGraph(attributePaths = {"batch", "student", "attachment"})
    List<AcademyRecord> findByBatchIdAndTypeAndActiveTrueOrderByCreatedAtDesc(Long batchId, RecordType type);
}
