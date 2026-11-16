package com.ems.service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class EmployeePhotoService {

    private final Cloudinary cloudinary;

    public EmployeePhotoService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".webp");

    // Upload employee photo to Cloudinary
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

        String publicId = UUID.randomUUID().toString();

        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "ems/employees",
                        "public_id", publicId,
                        "resource_type", "image"
                )
        );

        return (String) result.get("secure_url");
    }

    // Delete employee photo from Cloudinary
    public void deletePhoto(String photoUrl) throws IOException {

        if (photoUrl == null || photoUrl.isBlank()) {
            return;
        }

        // Ignore old local filenames during migration
        if (!photoUrl.startsWith("https://res.cloudinary.com/")) {
            return;
        }

        String marker = "/upload/";
        int uploadIndex = photoUrl.indexOf(marker);

        if (uploadIndex == -1) {
            throw new IllegalArgumentException("Invalid Cloudinary URL.");
        }

        String publicId = photoUrl.substring(
                uploadIndex + marker.length());

        // Remove version number if present
        publicId = publicId.replaceFirst("^v[0-9]+/", "");

        // Remove image extension
        int extensionIndex = publicId.lastIndexOf(".");

        if (extensionIndex != -1) {
            publicId = publicId.substring(0, extensionIndex);
        }

        if (!publicId.startsWith("ems/employees/")) {
            throw new IllegalArgumentException("Invalid employee photo.");
        }

        cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap("resource_type", "image")
        );
    }
}