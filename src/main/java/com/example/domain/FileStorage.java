/**
 * Author: komiloff_doniyor2505@gmail.com
 * Date:4/4/2023
 * Time:9:15 PM
 * Project Name:file-storage-amazon-s3-tutorial
 */
package com.example.domain;

import com.example.domain.enums.FileStorageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "file_storages")
public class FileStorage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String contentType;

    private String extension;

    private long size;

    private String hashId;

    private String uploadPath;

    private FileStorageStatus status;
}
