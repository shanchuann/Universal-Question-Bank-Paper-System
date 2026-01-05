package com.universal.qbank.repository;

import com.universal.qbank.entity.PermissionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, String> {

  Optional<PermissionEntity> findByCode(String code);

  List<PermissionEntity> findByResource(String resource);

  boolean existsByCode(String code);
}
