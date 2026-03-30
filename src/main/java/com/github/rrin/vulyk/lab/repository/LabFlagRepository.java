package com.github.rrin.vulyk.lab.repository;

import com.github.rrin.vulyk.lab.entity.LabFlagEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabFlagRepository extends JpaRepository<LabFlagEntity, Long> {

    List<LabFlagEntity> findAllByLabId(String labId);

    Optional<LabFlagEntity> findByLabIdAndTaskId(String labId, String taskId);
}