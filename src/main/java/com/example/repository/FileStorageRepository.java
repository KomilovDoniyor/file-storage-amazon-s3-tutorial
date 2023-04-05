package com.example.repository;

import com.example.domain.FileStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileStorageRepository extends JpaRepository<FileStorage, Long> {
    Optional<FileStorage> findByHashId(String hashId);
}
