package com.club.order.service;

import com.club.order.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path root;

    public LocalFileStorageService(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的图片");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot).toLowerCase(Locale.ROOT);
        }
        if (!List.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(ext)) {
            throw new BizException(400, "仅支持 jpg/png/webp/gif 图片");
        }
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            file.transferTo(root.resolve(name).toFile());
        } catch (IOException e) {
            throw new BizException(500, "图片保存失败");
        }
        return "/uploads/" + name;
    }
}
