package org.tonyqwe.cinemaweb.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tonyqwe.cinemaweb.config.MinioConfig;

import java.io.InputStream;
import java.util.UUID;

@Service
public class FileUploadService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    public String uploadAvatar(MultipartFile file) throws Exception {
        // 检查桶是否存在，不存在则创建
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucketName()).build());
        }

        // 生成唯一文件名
        String fileName = "avatar/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        // 上传文件
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }

        // 返回访问URL
        return minioConfig.getUrlPrefix() + "/" + fileName;
    }
}