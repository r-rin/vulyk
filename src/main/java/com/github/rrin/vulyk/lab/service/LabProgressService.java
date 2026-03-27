package com.github.rrin.vulyk.lab.service;

import com.github.rrin.vulyk.exception.NotFoundException;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.lab.domain.LabApiStatusResponse;
import com.github.rrin.vulyk.lab.domain.LabApiTaskStatus;
import com.github.rrin.vulyk.lab.domain.LabBoardView;
import com.github.rrin.vulyk.lab.domain.LabCardView;
import com.github.rrin.vulyk.lab.domain.LabDefinition;
import com.github.rrin.vulyk.lab.domain.LabProgressStatus;
import com.github.rrin.vulyk.lab.domain.LabTaskDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskHintDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskHintView;
import com.github.rrin.vulyk.lab.domain.LabTaskMode;
import com.github.rrin.vulyk.lab.domain.LabTaskProgressStatus;
import com.github.rrin.vulyk.lab.domain.LabTaskView;
import com.github.rrin.vulyk.lab.entity.LabFlagEntity;
import com.github.rrin.vulyk.lab.entity.LabHintRevealEntity;
import com.github.rrin.vulyk.lab.entity.LabTaskProgressEntity;
import com.github.rrin.vulyk.lab.repository.LabFlagRepository;
import com.github.rrin.vulyk.lab.repository.LabHintRevealRepository;
import com.github.rrin.vulyk.lab.repository.LabTaskProgressRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabProgressService {

    private final List<LabDefinition> activeLabs;
    private final LabFlagRepository labFlagRepository;
    private final LabHintRevealRepository labHintRevealRepository;
    private final LabTaskProgressRepository labTaskProgressRepository;

    @Transactional
    public LabBoardView getBoardView() {
        List<LabCardView> cards = activeLabs.stream()
            .sorted(Comparator.comparing(LabDefinition::getId))
            .map(this::toCardView)
            .toList();

        int totalPoints = cards.stream().mapToInt(LabCardView::points).sum();
        int earnedPoints = cards.stream().mapToInt(LabCardView::pointsEarned).sum();
        int completedCount = (int) cards.stream().filter(LabCardView::completed).count();

        return new LabBoardView(totalPoints, earnedPoints, completedCount, cards.size(), cards);
    }

    @Transactional
    public LabCardView getLabCard(String labId) {
        return toCardView(requireActiveLab(labId));
    }

    @Transactional
    public List<LabApiStatusResponse> getAllStatuses() {
        return activeLabs.stream()
            .sorted(Comparator.comparing(LabDefinition::getId))
            .map(this::toApiStatus)
            .toList();
    }

    @Transactional
    public LabApiStatusResponse getStatus(String labId) {
        return toApiStatus(requireActiveLab(labId));
    }

    @Transactional
    public LabApiStatusResponse submitFlag(String labId, String flagValue) {
        LabDefinition labDefinition = requireActiveLab(labId);
        ensureProgressRows(labDefinition);

        String normalizedFlag = normalizeFlag(flagValue);
        LabFlagEntity expectedFlag = labFlagRepository.findByLabIdAndFlagValue(labDefinition.getId(), normalizedFlag)
            .orElseThrow(() -> new ValidationException("Flag was not accepted for this lab"));

        LabTaskProgressEntity progress = labTaskProgressRepository.findByLabIdAndTaskId(
            expectedFlag.getLabId(),
            expectedFlag.getTaskId()
        ).orElseThrow(() -> new ValidationException("Task progress was not initialized"));

        if (progress.getStatus() != LabTaskProgressStatus.COMPLETED) {
            LabTaskDefinition taskDefinition = labDefinition.getTasks().stream()
                .filter(task -> task.id().equals(expectedFlag.getTaskId()))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Task definition was not found"));

            int awardedPoints = calculateMaxPointsAvailable(labDefinition.getId(), taskDefinition);

            progress.setStatus(LabTaskProgressStatus.COMPLETED);
            progress.setPointsAwarded(awardedPoints);
            progress.setCompletedAt(Instant.now());
            progress.setEvidence("flag:" + expectedFlag.getFlagValue());
            labTaskProgressRepository.save(progress);
        }

        return toApiStatus(labDefinition);
    }

    @Transactional
    public void revealHint(String labId, String taskId, String hintId) {
        LabDefinition labDefinition = requireActiveLab(labId);
        LabTaskDefinition taskDefinition = requireTask(labDefinition, taskId);
        requireHint(taskDefinition, hintId);

        labTaskProgressRepository.findByLabIdAndTaskId(labDefinition.getId(), taskDefinition.id())
            .filter(progress -> progress.getStatus() == LabTaskProgressStatus.COMPLETED)
            .ifPresent(progress -> {
                throw new ValidationException("Hints cannot be revealed after the task has been completed");
            });

        labHintRevealRepository.findByLabIdAndTaskIdAndHintId(labDefinition.getId(), taskDefinition.id(), hintId)
            .orElseGet(() -> labHintRevealRepository.save(LabHintRevealEntity.builder()
                .labId(labDefinition.getId())
                .taskId(taskDefinition.id())
                .hintId(hintId)
                .build()));
    }

    @Transactional
    public void ensureProgressRows(LabDefinition labDefinition) {
        Map<String, LabTaskProgressEntity> existingByTask = labTaskProgressRepository.findAllByLabId(labDefinition.getId())
            .stream()
            .collect(LinkedHashMap::new, (map, item) -> map.put(item.getTaskId(), item), Map::putAll);

        for (LabTaskDefinition task : labDefinition.getTasks()) {
            if (existingByTask.containsKey(task.id())) {
                continue;
            }

            labTaskProgressRepository.save(LabTaskProgressEntity.builder()
                .labId(labDefinition.getId())
                .taskId(task.id())
                .status(LabTaskProgressStatus.PENDING)
                .pointsAwarded(0)
                .build());
        }
    }

    private LabCardView toCardView(LabDefinition labDefinition) {
        ensureProgressRows(labDefinition);
        Map<String, LabTaskProgressEntity> progressByTask = progressByTask(labDefinition.getId());
        Map<String, List<LabHintRevealEntity>> hintsByTask = hintRevealsByTask(labDefinition.getId());

        List<LabTaskView> tasks = labDefinition.getTasks().stream()
            .map(task -> {
                LabTaskProgressEntity progress = progressByTask.get(task.id());
                List<LabHintRevealEntity> reveals = hintsByTask.getOrDefault(task.id(), List.of());
                List<LabTaskHintView> hints = task.hints().stream()
                    .map(hint -> new LabTaskHintView(
                        hint.id(),
                        hint.title(),
                        hint.content(),
                        hint.penalty(),
                        reveals.stream().anyMatch(reveal -> reveal.getHintId().equals(hint.id()))
                    ))
                    .toList();
                int maxPointsAvailable = calculateMaxPointsAvailable(task, hints);
                int pointsEarned = progress != null ? progress.getPointsAwarded() : 0;
                LabTaskProgressStatus status = progress != null ? progress.getStatus() : LabTaskProgressStatus.PENDING;
                return new LabTaskView(
                    task.id(),
                    task.title(),
                    task.description(),
                    task.points(),
                    maxPointsAvailable,
                    pointsEarned,
                    task.mode(),
                    status,
                    hints
                );
            })
            .toList();

        int pointsEarned = tasks.stream().mapToInt(LabTaskView::pointsEarned).sum();
        boolean completed = tasks.stream().allMatch(task -> task.status() == LabTaskProgressStatus.COMPLETED);
        boolean hasFlagTask = tasks.stream().anyMatch(task -> task.mode() == LabTaskMode.FLAG_SUBMISSION);

        return new LabCardView(
            labDefinition.getId(),
            labDefinition.getTitle(),
            labDefinition.getCategory(),
            labDefinition.getDescription(),
            labDefinition.getEntryPath(),
            labDefinition.getTotalPoints(),
            pointsEarned,
            completed ? LabProgressStatus.COMPLETED : LabProgressStatus.ACTIVE,
            hasFlagTask,
            completed,
            tasks
        );
    }

    private LabApiStatusResponse toApiStatus(LabDefinition labDefinition) {
        LabCardView card = toCardView(labDefinition);
        List<LabApiTaskStatus> tasks = card.tasks().stream()
            .map(task -> new LabApiTaskStatus(
                task.id(),
                task.title(),
                task.description(),
                task.mode(),
                task.status(),
                task.points(),
                task.maxPointsAvailable(),
                task.pointsEarned()
            ))
            .toList();

        return new LabApiStatusResponse(
            card.id(),
            card.title(),
            card.category(),
            card.status(),
            card.pointsEarned(),
            card.points(),
            tasks
        );
    }

    private LabDefinition requireActiveLab(String labId) {
        String normalized = labId == null ? "" : labId.trim().toUpperCase(Locale.ROOT);
        return activeLabs.stream()
            .filter(lab -> lab.getId().equalsIgnoreCase(normalized))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Lab is not active in this instance"));
    }

    private Map<String, LabTaskProgressEntity> progressByTask(String labId) {
        return labTaskProgressRepository.findAllByLabId(labId).stream()
            .collect(LinkedHashMap::new, (map, item) -> map.put(item.getTaskId(), item), Map::putAll);
    }

    private Map<String, List<LabHintRevealEntity>> hintRevealsByTask(String labId) {
        return labHintRevealRepository.findAllByLabId(labId).stream()
            .collect(LinkedHashMap::new, (map, item) -> map.computeIfAbsent(item.getTaskId(), ignored -> new java.util.ArrayList<>()).add(item), Map::putAll);
    }

    private int calculateMaxPointsAvailable(String labId, LabTaskDefinition taskDefinition) {
        List<LabTaskHintView> hints = taskDefinition.hints().stream()
            .map(hint -> new LabTaskHintView(
                hint.id(),
                hint.title(),
                hint.content(),
                hint.penalty(),
                labHintRevealRepository.findByLabIdAndTaskIdAndHintId(labId, taskDefinition.id(), hint.id()).isPresent()
            ))
            .toList();
        return calculateMaxPointsAvailable(taskDefinition, hints);
    }

    private int calculateMaxPointsAvailable(LabTaskDefinition taskDefinition, List<LabTaskHintView> hints) {
        int penalty = hints.stream()
            .filter(LabTaskHintView::revealed)
            .mapToInt(LabTaskHintView::penalty)
            .sum();
        return Math.max(taskDefinition.points() - penalty, 0);
    }

    private LabTaskDefinition requireTask(LabDefinition labDefinition, String taskId) {
        return labDefinition.getTasks().stream()
            .filter(task -> task.id().equals(taskId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Task is not active in this instance"));
    }

    private LabTaskHintDefinition requireHint(LabTaskDefinition taskDefinition, String hintId) {
        return taskDefinition.hints().stream()
            .filter(hint -> hint.id().equals(hintId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Hint is not defined for this task"));
    }

    private String normalizeFlag(String flagValue) {
        if (flagValue == null || flagValue.isBlank()) {
            throw new ValidationException("Flag value is required");
        }
        return flagValue.trim();
    }
}