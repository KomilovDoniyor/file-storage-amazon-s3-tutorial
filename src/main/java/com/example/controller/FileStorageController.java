/**
 * Author: komiloff_doniyor2505@gmail.com
 * Date:4/4/2023
 * Time:11:10 PM
 * Project Name:file-storage-amazon-s3-tutorial
 */
package com.example.controller;

import com.example.domain.FileStorage;
import com.example.repository.FileStorageRepository;
import com.example.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.util.UriEncoder;

import java.net.MalformedURLException;
import java.nio.file.FileSystemNotFoundException;

@RestController
@RequestMapping("/api/v1/file-storage")
@RequiredArgsConstructor
public class FileStorageController {

    private final FileStorageService fileStorageService;
    private final FileStorageRepository fileStorageRepository;

    @Value("${system.folder.path}")
    private String systemFolderPath;

    @Value("${application.bucket.name}")
    private String bucketName;

    @PostMapping("/upload")
    public ResponseEntity<?> save(@RequestParam("file")MultipartFile multipartFile,
                                  @RequestParam("status") boolean status) {
        FileStorage fileStorage = fileStorageService.save(multipartFile, status);
        return ResponseEntity.ok(fileStorage);
    }

    @GetMapping("/preview/{hashId}")
    public ResponseEntity<?> view(@PathVariable("hashId") String hashId) throws MalformedURLException {
        FileStorage fileStorage = fileStorageService.view(hashId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; fileName=\"" + UriEncoder.encode(fileStorage.getName()))
                .contentType(MediaType.parseMediaType(fileStorage.getContentType()))
                .contentLength(fileStorage.getSize())
                .body(new FileUrlResource(String.format("%s/%s", this.systemFolderPath, fileStorage.getUploadPath())));

    }

    @GetMapping("/download/{hashId}")
    public ResponseEntity<?> download(@PathVariable("hashId") String hashId) throws MalformedURLException {
        FileStorage fileStorage = fileStorageService.view(hashId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; fileName=\"" + UriEncoder.encode(fileStorage.getName()))
                .contentType(MediaType.parseMediaType(fileStorage.getContentType()))
                .contentLength(fileStorage.getSize())
                .body(new FileUrlResource(String.format("%s/%s", systemFolderPath, fileStorage.getUploadPath())));
    }

    @DeleteMapping("/delete/{hashId}")
    public ResponseEntity<?> delete(@PathVariable("hashId") String hashId) {
        fileStorageService.delete(hashId);
        return ResponseEntity.ok("File deleted");
    }


    @GetMapping("/download/awsS3/{hashId}")
    public ResponseEntity<?> downloadAwsS3(@PathVariable("hashId") String hashId) {
        FileStorage fileStorage = fileStorageRepository.findByHashId(hashId)
                .orElseThrow(() -> new FileSystemNotFoundException("File not found"));
        byte[] data = fileStorageService.downloadFileFromAws3(hashId);
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .contentLength(data.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; fileName=\"" + fileStorage.getName())
                .body(resource);
    }

    @DeleteMapping("/delete/awsS3/{hashId}")
    public ResponseEntity<?> deleteAwsS3(@PathVariable("hashId") String hashId) {
        return new ResponseEntity<>(fileStorageService.deleteFileAws3(hashId), HttpStatus.OK);
    }
}
