package com.ems.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmployeePhotoService {

    private final Path uploadDirectory =
            Paths.get("uploads", "employees")
                    .toAbsolutePath()
                    .normalize();

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".webp");

    // Save employee photo
    public String savePhoto(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            return null;
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "Only JPG, JPEG, PNG and WEBP images are allowed.");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Invalid image filename.");
        }

        String extension = originalFilename.substring(
                originalFilename.lastIndexOf(".")).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Invalid image extension.");
        }

        Files.createDirectories(uploadDirectory);

        String newFilename = UUID.randomUUID() + extension;

        Path destination =
                uploadDirectory.resolve(newFilename).normalize();

        if (!destination.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Invalid file path.");
        }

        Files.copy(file.getInputStream(), destination);

        return newFilename;
    }

    // Delete old employee photo
    public void deletePhoto(String filename) throws IOException {

        if (filename == null || filename.isBlank()) {
            return;
        }

        Path photoPath = uploadDirectory.resolve(filename).normalize();

        // Ensure the file belongs directly to the employee upload folder
        if (!photoPath.startsWith(uploadDirectory)
                || !uploadDirectory.equals(photoPath.getParent())) {

            throw new IllegalArgumentException(
                    "Invalid photo file path.");
        }

        Files.deleteIfExists(photoPath);
    }
}