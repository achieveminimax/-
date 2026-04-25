package com.seckill.admin.controller;

import com.seckill.admin.annotation.RequireAdmin;
import com.seckill.admin.enums.AdminRoleEnum;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 管理端文件上传入口。
 * <p>
 * 提供图片上传功能，用于商品图片、活动封面图等。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
public class AdminUploadController {

    /**
     * 允许上传的图片格式。
     */
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    /**
     * 允许的文件扩展名。
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    /**
     * 最大文件大小：5MB。
     */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Value("${app.upload.path:uploads/images/}")
    private String uploadPath;

    @Value("${app.upload.base-url:http://localhost:8080/uploads/}")
    private String baseUrl;

    /**
     * 上传图片。
     *
     * @param image 图片文件
     * @return 图片访问 URL
     */
    @PostMapping("/image")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<Map<String, String>> uploadImage(@RequestParam("image") MultipartFile image) {
        // 校验文件是否为空
        if (image.isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "请选择要上传的文件");
        }

        // 校验文件大小
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResponseCodeEnum.FILE_SIZE_EXCEED, "文件大小不能超过 5MB");
        }

        // 校验文件类型
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ResponseCodeEnum.FILE_TYPE_NOT_SUPPORT, "仅支持 jpg、jpeg、png、webp 格式的图片");
        }

        // 校验文件扩展名
        String originalFilename = image.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "文件名不能为空");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResponseCodeEnum.FILE_TYPE_NOT_SUPPORT, "仅支持 jpg、jpeg、png、webp 格式的图片");
        }

        try {
            // 生成文件名
            String newFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            
            // 按日期创建子目录
            java.time.LocalDate now = java.time.LocalDate.now();
            String subDir = String.format("%d/%02d/%02d/", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
            String relativePath = subDir + newFilename;
            
            // 创建目录
            Path targetDir = Paths.get(uploadPath, subDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            // 保存文件
            Path targetPath = targetDir.resolve(newFilename);
            Files.copy(image.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // 生成访问 URL
            String imageUrl = baseUrl + relativePath;
            
            log.info("图片上传成功: {}", imageUrl);
            
            return Result.success(Map.of("imageUrl", imageUrl));
            
        } catch (IOException e) {
            log.error("图片上传失败", e);
            throw new BusinessException(ResponseCodeEnum.FILE_UPLOAD_ERROR, "图片上传失败，请稍后重试");
        }
    }

    /**
     * 获取文件扩展名。
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
