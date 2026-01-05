package com.universal.qbank.repository;

import com.universal.qbank.entity.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, String> {

  Optional<RoleEntity> findByCode(String code);

  boolean existsByCode(String code);
}
