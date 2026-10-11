package in.saisahayadri.academy.repository;

import in.saisahayadri.academy.model.AcademyBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchRepository extends JpaRepository<AcademyBatch, Long> {
    List<AcademyBatch> findByActiveTrueOrderByName();
}
