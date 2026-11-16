package com.marknote.controller;

import com.marknote.entity.Image;
import com.marknote.service.ImageService;
import com.marknote.util.Result;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/image")
public class ImageController {

    /** 允许上传的图片类型 */
    private static final List<String> ALLOWED_TYPE = Arrays.asList(
            "image/png", "image/jpeg", "image/jpg", "image/gif",
            "image/webp", "image/bmp", "image/svg+xml");

    private static final long MAX_SIZE = 10L * 1024 * 1024;

    @Resource
    private MinioClient minioClient;

    @Resource
    private ImageService imageService;

    @Value("${minio.bucket-name}")
    private String bucket;

    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

    /** MinIO 不可用时的本地兜底目录 */
    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.file-base-url:http://localhost:8080}")
    private String fileBaseUrl;

    /**
     * 上传图片，返回可直接访问的 URL。
     * 优先存 MinIO；MinIO 不可用时自动落到本地磁盘（/files/**），保证功能不中断。
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (file == null || file.isEmpty()) {
            return Result.fail("文件为空");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.fail("图片不能超过 10MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED_TYPE.contains(contentType)) {
            return Result.fail("只支持 png/jpg/jpeg/gif/webp/bmp/svg 图片");
        }

        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String uuidName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 1) 先试 MinIO
        try {
            ensureBucket();
            String objectName = userId + "/" + uuidName;
            try (InputStream in = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(in, file.getSize(), -1)
                        .contentType(contentType)
                        .build());
            }
            String url = publicEndpoint + "/" + bucket + "/" + objectName;
            saveRecord(userId, url, bucket, objectName);
            return Result.success(url);
        } catch (Exception minioError) {
            System.err.println("MinIO 上传失败，改用本地存储：" + minioError.getMessage());
        }

        // 2) 本地兜底
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(uuidName).normalize();
            if (!target.startsWith(dir)) {
                return Result.fail("非法文件名");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String url = fileBaseUrl + "/files/" + uuidName;
            saveRecord(userId, url, "local", uuidName);
            return Result.success(url);
        } catch (Exception localError) {
            localError.printStackTrace();
            return Result.fail("上传失败：" + localError.getMessage());
        }
    }

    private void saveRecord(Long userId, String url, String bucketName, String objectName) {
        Image img = new Image();
        img.setUserId(userId);
        img.setUrl(url);
        img.setBucket(bucketName);
        img.setObjectName(objectName);
        imageService.save(img);
    }

    /**
     * 确保 bucket 存在，并设置公开只读策略（否则浏览器无法直接显示图片）
     */
    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\","
                + "\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],"
                + "\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
    }
}
