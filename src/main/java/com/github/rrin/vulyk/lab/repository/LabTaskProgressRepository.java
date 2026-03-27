package com.github.rrin.vulyk.lab.repository;

import com.github.rrin.vulyk.lab.entity.LabTaskProgressEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabTaskProgressRepository extends JpaRepository<LabTaskProgressEntity, Long> {

    List<LabTaskProgressEntity> findAllByLabId(String labId);

    Optional<LabTaskProgressEntity> findByLabIdAndTaskId(String labId, String taskId);
}