package in.saisahayadri.academy.service;

import in.saisahayadri.academy.model.AcademyBatch;
import in.saisahayadri.academy.model.StudyMaterial;
import in.saisahayadri.academy.repository.BatchRepository;
import in.saisahayadri.academy.repository.StudyMaterialRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class StudyMaterialService {
    private static final long MAX_FILE_SIZE = 15L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "ppt", "pptx", "png", "jpg", "jpeg", "txt");

    private final StudyMaterialRepository materials;
    private final BatchRepository batches;
    private final Path storageDirectory;

    public StudyMaterialService(StudyMaterialRepository materials, BatchRepository batches,
                               @Value("${academy.materials.directory:./data/materials}") String storageDirectory) {
        this.materials = materials;
        this.batches = batches;
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public StudyMaterial upload(String title, String description, Long batchId, MultipartFile file) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("A material title is required.");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose a file to upload.");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Files must be 15 MB or smaller.");
        String originalName = Path.of(file.getOriginalFilename() == null ? "" : file.getOriginalFilename())
                .getFileName().toString().replaceAll("[\\r\\n\\t]", "_");
        int extensionDot = originalName.lastIndexOf('.');
        String extension = extensionDot < 0 ? "" : originalName.substring(extensionDot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Allowed file types: PDF, Word, PowerPoint, text, PNG and JPEG.");
        }
        AcademyBatch batch = batchId == null ? null : batches.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Selected batch was not found."));
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path target = storageDirectory.resolve(storedFilename).normalize();
        if (!target.startsWith(storageDirectory)) throw new IllegalStateException("Invalid material storage path.");
        try {
            Files.createDirectories(storageDirectory);
            file.transferTo(target);
            return materials.save(new StudyMaterial(title.trim(), description == null ? "" : description.trim(),
                    originalName, storedFilename, batch));
        } catch (IOException exception) {
            throw new IllegalStateException("The material could not be saved. Check the academy storage configuration.", exception);
        }
    }

    @Transactional
    public StudyMaterial uploadUpdateAttachment(String title, String description, Long batchId, MultipartFile file) {
        String filename = file == null ? "" : Path.of(file.getOriginalFilename() == null ? "" : file.getOriginalFilename())
                .getFileName().toString();
        String extension = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!Set.of("pdf", "png", "jpg", "jpeg").contains(extension)) {
            throw new IllegalArgumentException("Academy update attachments must be a PDF, PNG or JPEG image.");
        }
        return upload(title, description, batchId, file);
    }

    @Transactional
    public void deactivate(Long id) {
        StudyMaterial material = materials.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Study material was not found."));
        material.setActive(false);
    }

    public List<StudyMaterial> forAdmin() {
        return materials.findByActiveTrueOrderByCreatedAtDesc();
    }

    public List<StudyMaterial> forStudent(Long batchId) {
        List<StudyMaterial> visible = new java.util.ArrayList<>(materials.findByActiveTrueOrderByCreatedAtDesc()
                .stream().filter(material -> material.getBatch() == null).toList());
        if (batchId != null) visible.addAll(materials.findByBatchIdAndActiveTrueOrderByCreatedAtDesc(batchId));
        return visible;
    }

    public Optional<Path> fileForAdmin(Long id) {
        return materials.findByIdAndActiveTrue(id).map(material -> storageDirectory.resolve(material.getStoredFilename()).normalize())
                .filter(path -> path.startsWith(storageDirectory) && Files.isRegularFile(path));
    }

    public Optional<Path> fileForStudent(Long id, Long batchId) {
        return materials.findByIdAndActiveTrue(id)
                .filter(material -> material.getBatch() == null
                        || (batchId != null && material.getBatch().getId().equals(batchId)))
                .map(material -> storageDirectory.resolve(material.getStoredFilename()).normalize())
                .filter(path -> path.startsWith(storageDirectory) && Files.isRegularFile(path));
    }

    public Optional<StudyMaterial> findActive(Long id) {
        return materials.findByIdAndActiveTrue(id);
    }
}
