/**
 * Author: komiloff_doniyor2505@gmail.com
 * Date:4/4/2023
 * Time:9:22 PM
 * Project Name:file-storage-amazon-s3-tutorial
 */
package com.example.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import com.example.domain.FileStorage;
import com.example.domain.enums.FileStorageStatus;
import com.example.repository.FileStorageRepository;
import lombok.extern.slf4j.Slf4j;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.util.Date;

@Service
@Slf4j
public class FileStorageService {

    private final FileStorageRepository fileStorageRepository;
    private final Hashids hashids;
    private final AmazonS3 amazonS3;

    @Value("${system.folder.path}")
    private String systemFolderPath;

    @Value("${application.bucket.name}")
    private String bucketName;

    public FileStorageService(FileStorageRepository fileStorageRepository, AmazonS3 amazonS3) {
        this.fileStorageRepository = fileStorageRepository;
        this.amazonS3 = amazonS3;
        this.hashids = new Hashids(getClass().getName(), 10);
    }

    public FileStorage save(MultipartFile multipartFile, boolean status) {
        FileStorage fileStorage = new FileStorage();
        fileStorage.setName(multipartFile.getOriginalFilename());
        fileStorage.setContentType(multipartFile.getContentType());
        fileStorage.setExtension(getExt(multipartFile.getOriginalFilename()));
        fileStorage.setSize(multipartFile.getSize());
        fileStorage.setStatus(FileStorageStatus.DRAFT);
        fileStorage = fileStorageRepository.save(fileStorage);
        fileStorage.setHashId(hashids.encode(fileStorage.getId()));

        // Example: systemFolderPath/upload_folder/2023/04/04/filename.doc

        if (!status) {
            Date date = new Date();
            String path = String.format(
                    "%s/upload_file/%d/%d/%d", this.systemFolderPath,
                    (1900 + date.getYear()),
                    (1 + date.getMonth()),
                    date.getDay() + 2);
            File uploadFolder = new File(path);

            if (!uploadFolder.exists() && uploadFolder.mkdirs()) {
                log.info("Created folder");
            }
            fileStorage.setUploadPath(
                    String.format("/upload_file/%d/%d/%d/%s.%s",
                            (1900 + date.getYear()),
                            (1 + date.getMonth()),
                            date.getDay() + 2,
                            fileStorage.getHashId(),
                            fileStorage.getExtension()));
            fileStorageRepository.save(fileStorage);

            uploadFolder = uploadFolder.getAbsoluteFile();

            File file = new File(
                    uploadFolder,
                    String.format("%s.%s", fileStorage.getHashId(), fileStorage.getExtension()));
            try {
                multipartFile.transferTo(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return fileStorage;
        }

        //================= FOR AMAZON S3 =======================

        //https://filestorage-example.s3.us-east-2.amazonaws.com/Weather.jpg
        Date date = new Date();
        File convertMultipartFileToFile = convertMultipartFileToFile(multipartFile);
        String awsPath = String.format("upload_file/%d/%d/%d/%s.%s",
                (1900 + date.getYear()),
                (1 + date.getMonth()),
                date.getDay() + 2,
                fileStorage.getHashId(),
                fileStorage.getExtension());
        amazonS3.putObject(new PutObjectRequest(
                bucketName,
                awsPath,
                convertMultipartFileToFile));
        convertMultipartFileToFile.delete();
        fileStorage.setUploadPath(awsPath);
        fileStorageRepository.save(fileStorage);

        //================= FOR AMAZON S3 =======================

        return fileStorage;
    }



    private String getExt(String fileName) {
        String extension = null;

        // hisobot.doc

        if (fileName != null && !fileName.isEmpty()) {
            int dot = fileName.lastIndexOf(".");

            // Bu yerda fileName.length() - 2 deganimiz extensionlarimiz eng kamida 2 bo'lishini bildiradi!

            if (dot > 0 && dot < fileName.length() - 2) {
                extension = fileName.substring(dot + 1);
            }
        }
        return extension;
    }


    public FileStorage view(String hashId) {
        return fileStorageRepository.findByHashId(hashId)
                .orElseThrow(() -> new FileSystemNotFoundException("File not found"));
    }

    public void delete(String hashId) {
        FileStorage fileStorage = fileStorageRepository.findByHashId(hashId)
                .orElseThrow(() -> new FileSystemNotFoundException("File not found"));
        File file = new File(String.format("%s/%s", this.systemFolderPath, fileStorage.getUploadPath()));
        if (file.delete()) {
            fileStorageRepository.delete(fileStorage);
        }
    }

    //================= FOR AMAZON S3 =======================

    private File convertMultipartFileToFile(MultipartFile multipartFile) {
        File convertedFile = new File(multipartFile.getOriginalFilename());
        try (FileOutputStream fileOutputStream = new FileOutputStream(convertedFile)){
            fileOutputStream.write(multipartFile.getBytes());
        } catch (IOException e) {
            log.info("Error converting multipartFile to file", e);
            e.printStackTrace();
        }
        return convertedFile;
    }

    public byte[] downloadFileFromAws3(String hashId) {
        Date date = new Date();
        String awsPath = String.format("upload_file/%d/%d/%d/%s.%s",
                (1900 + date.getYear()),
                (1 + date.getMonth()),
                date.getDay() + 2);
        S3Object s3Object = amazonS3.getObject(bucketName + awsPath, hashId);
        S3ObjectInputStream objectContent = s3Object.getObjectContent();
        try {
            byte[] content = IOUtils.toByteArray(objectContent);
            return content;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String deleteFileAws3(String fileName) {
        amazonS3.deleteObject(bucketName, fileName);
        return fileName + " deleted!!!";
    }

    //================= FOR AMAZON S3 =======================
}
