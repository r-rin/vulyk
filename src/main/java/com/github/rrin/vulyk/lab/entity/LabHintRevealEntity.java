package com.github.rrin.vulyk.lab.entity;

import com.github.rrin.vulyk.domain.Identifiable;
import com.github.rrin.vulyk.domain.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "lab_hint_reveals", uniqueConstraints = {
    @UniqueConstraint(name = "uc_lab_hint_reveals_lab_task_hint", columnNames = {"lab_id", "task_id", "hint_id"})
}, indexes = {
    @Index(name = "idx_lab_hint_reveals_lab_task", columnList = "lab_id, task_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LabHintRevealEntity extends AuditableEntity implements Identifiable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "lab_id", nullable = false, length = 64)
    private String labId;

    @Column(name = "task_id", nullable = false, length = 128)
    private String taskId;

    @Column(name = "hint_id", nullable = false, length = 128)
    private String hintId;
}