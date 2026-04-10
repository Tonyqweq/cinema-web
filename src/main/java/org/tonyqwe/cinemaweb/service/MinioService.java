package org.tonyqwe.cinemaweb.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class MinioService {

    private static final Logger logger = LoggerFactory.getLogger(MinioService.class);
    
    private final MinioClient minioClient;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    @Value("${minio.url-prefix}")
    private String urlPrefix;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @PostConstruct
    private void initBucket() {
        try {
            logger.info("开始初始化MinIO存储桶: {}", bucketName);
            
            // 检查桶是否存在
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            
            logger.info("存储桶 {} 存在: {}", bucketName, exists);
            
            // 如果桶不存在，创建桶
            if (!exists) {
                logger.info("创建存储桶: {}", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                logger.info("存储桶创建成功: {}", bucketName);
            }
            
            // 设置桶的访问策略，允许公开访问
            String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]}]}";
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build());
            logger.info("存储桶访问策略设置成功: {}", bucketName);
        } catch (Exception e) {
            logger.error("初始化MinIO存储桶失败: {}", e.getMessage(), e);
            // 继续启动应用，不因为MinIO连接失败而停止
            logger.warn("MinIO连接失败，应用将继续启动，但海报上传功能可能不可用");
        }
    }

    public String uploadFile(MultipartFile file) throws IOException {
        try {
            logger.info("开始上传文件: {}", file.getOriginalFilename());
            
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            logger.info("生成文件名: {}", fileName);
            
            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            
            // 返回文件URL
            String fileUrl = urlPrefix + "/" + fileName;
            logger.info("文件上传成功，URL: {}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            throw new IOException("文件上传失败", e);
        }
    }
}
