package com.github.rrin.vulyk.lab.entity;

import com.github.rrin.vulyk.domain.Identifiable;
import com.github.rrin.vulyk.domain.entity.AuditableEntity;
import com.github.rrin.vulyk.lab.domain.LabTaskProgressStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "lab_task_progress", uniqueConstraints = {
    @UniqueConstraint(name = "uc_lab_task_progress_lab_task", columnNames = {"lab_id", "task_id"})
}, indexes = {
    @Index(name = "idx_lab_task_progress_lab", columnList = "lab_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LabTaskProgressEntity extends AuditableEntity implements Identifiable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "lab_id", nullable = false, length = 64)
    private String labId;

    @Column(name = "task_id", nullable = false, length = 128)
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private LabTaskProgressStatus status;

    @Column(name = "points_awarded", nullable = false)
    private int pointsAwarded;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "evidence", length = 255)
    private String evidence;
}