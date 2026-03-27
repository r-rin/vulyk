package com.github.rrin.vulyk.lab.repository;

import com.github.rrin.vulyk.lab.entity.LabHintRevealEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabHintRevealRepository extends JpaRepository<LabHintRevealEntity, Long> {

    List<LabHintRevealEntity> findAllByLabId(String labId);

    List<LabHintRevealEntity> findAllByLabIdAndTaskId(String labId, String taskId);

    Optional<LabHintRevealEntity> findByLabIdAndTaskIdAndHintId(String labId, String taskId, String hintId);
}