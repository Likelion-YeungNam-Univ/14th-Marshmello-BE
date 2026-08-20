package Marshmello.MarshmelloWas.domain.care.service;

import Marshmello.MarshmelloWas.domain.checkin.entity.ImageAnalysis;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageAnalysisRepository;
import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.care.dto.CareCardCreationResult;
import Marshmello.MarshmelloWas.domain.care.dto.CareCardGenerationRequest;
import Marshmello.MarshmelloWas.domain.care.dto.CareCardResponse;
import Marshmello.MarshmelloWas.domain.care.entity.Action;
import Marshmello.MarshmelloWas.domain.care.entity.CareCard;
import Marshmello.MarshmelloWas.domain.care.entity.UserActionFeedback;
import Marshmello.MarshmelloWas.domain.care.entity.UserActionFeedbackId;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator.CareCardGeneratedText;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator.CareCardGenerationException;
import Marshmello.MarshmelloWas.domain.care.repository.ActionRepository;
import Marshmello.MarshmelloWas.domain.care.repository.CareCardRepository;
import Marshmello.MarshmelloWas.domain.care.repository.UserActionFeedbackRepository;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInImageReference;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CareCardService {

    private final CheckInRepository checkInRepository;
    private final ImageRepository imageRepository;
    private final ImageAnalysisRepository imageAnalysisRepository;
    private final ActionRepository actionRepository;
    private final CareCardRepository careCardRepository;
    private final UserActionFeedbackRepository feedbackRepository;
    private final CurrentUserIdProvider currentUserIdProvider;
    private final CareCardGenerator careCardGenerator;
    private final ActionSelector actionSelector;
    private final TransactionTemplate readTransactions;
    private final TransactionTemplate writeTransactions;
    private final Clock clock;

    public CareCardService(
            CheckInRepository checkInRepository,
            ImageRepository imageRepository,
            ImageAnalysisRepository imageAnalysisRepository,
            ActionRepository actionRepository,
            CareCardRepository careCardRepository,
            UserActionFeedbackRepository feedbackRepository,
            CurrentUserIdProvider currentUserIdProvider,
            CareCardGenerator careCardGenerator,
            ActionSelector actionSelector,
            PlatformTransactionManager transactionManager,
            Clock clock
    ) {
        this.checkInRepository = checkInRepository;
        this.imageRepository = imageRepository;
        this.imageAnalysisRepository = imageAnalysisRepository;
        this.actionRepository = actionRepository;
        this.careCardRepository = careCardRepository;
        this.feedbackRepository = feedbackRepository;
        this.currentUserIdProvider = currentUserIdProvider;
        this.careCardGenerator = careCardGenerator;
        this.actionSelector = actionSelector;
        this.readTransactions = new TransactionTemplate(transactionManager);
        this.readTransactions.setReadOnly(true);
        this.writeTransactions = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public CareCardCreationResult create(long checkInId) {
        long userId = currentUserIdProvider.requireCurrentUserId();
        Preparation preparation = requiredTransactionResult(
                readTransactions.execute(status -> prepare(checkInId, userId)));
        if (preparation instanceof ExistingCard existingCard) {
            return new CareCardCreationResult(existingCard.careCard(), false);
        }

        GenerationContext context = (GenerationContext) preparation;
        CareCardGeneratedText generatedText = generate(context);
        try {
            return requiredTransactionResult(writeTransactions.execute(
                    status -> saveGeneratedCard(context, generatedText)));
        } catch (DataIntegrityViolationException exception) {
            CareCardResponse existing = readExistingCard(checkInId, userId);
            if (existing != null) {
                return new CareCardCreationResult(existing, false);
            }
            throw exception;
        }
    }

    public CareCardResponse getByCheckInId(long checkInId) {
        long userId = currentUserIdProvider.requireCurrentUserId();
        return requiredTransactionResult(readTransactions.execute(status -> {
            requireOwnedCheckIn(checkInId, userId, ErrorCode.CHECK_IN_NOT_FOUND);
            return careCardRepository.findByCheckInId(checkInId)
                    .map(this::toResponse)
                    .orElseThrow(() -> new ApiException(ErrorCode.CARE_CARD_NOT_FOUND));
        }));
    }

    public CareCardResponse getLatest() {
        long userId = currentUserIdProvider.requireCurrentUserId();
        return requiredTransactionResult(readTransactions.execute(status -> careCardRepository
                .findLatestByUserId(userId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.CARE_CARD_NOT_FOUND))));
    }

    public void updateFeedback(long careCardId, short helpfulnessScore) {
        long userId = currentUserIdProvider.requireCurrentUserId();
        writeTransactions.executeWithoutResult(status -> {
            CareCard careCard = requireOwnedCareCard(careCardId, userId);
            UserActionFeedbackId feedbackId = new UserActionFeedbackId(
                    userId,
                    careCard.getAction().getActionId());
            feedbackRepository.findById(feedbackId)
                    .ifPresentOrElse(
                            feedback -> feedback.updateHelpfulnessScore(helpfulnessScore),
                            () -> feedbackRepository.save(new UserActionFeedback(feedbackId, helpfulnessScore)));
        });
    }

    private Preparation prepare(long checkInId, long userId) {
        requireOwnedCheckIn(checkInId, userId, ErrorCode.CHECK_IN_NOT_FOUND);
        CareCard existing = careCardRepository.findByCheckInId(checkInId).orElse(null);
        if (existing != null) {
            return new ExistingCard(toResponse(existing));
        }

        CheckInImageReference image = imageRepository.findReferenceByCheckInId(checkInId)
                .orElseThrow(this::actionInvariantViolation);
        ImageAnalysis analysis = imageAnalysisRepository.findById(image.imageId())
                .orElseThrow(this::actionInvariantViolation);
        List<Action> actions = actionRepository.findByActionScoreOrderByActionIdAsc(analysis.score());
        if (actions.isEmpty()) {
            throw actionInvariantViolation();
        }

        Map<Long, Short> feedbackScores = feedbackRepository
                .findByFeedbackIdUserIdAndFeedbackIdActionIdIn(
                        userId,
                        actions.stream().map(Action::getActionId).toList())
                .stream()
                .collect(Collectors.toMap(
                        feedback -> feedback.getFeedbackId().getActionId(),
                        UserActionFeedback::getHelpfulnessScore));
        List<ActionSelector.Candidate> candidates = actions.stream()
                .map(action -> new ActionSelector.Candidate(
                        action.getActionId(),
                        feedbackScores.get(action.getActionId())))
                .toList();
        long selectedActionId = actionSelector.select(candidates).actionId();
        Map<Long, Action> actionsById = actions.stream()
                .collect(Collectors.toMap(Action::getActionId, Function.identity()));
        Action selectedAction = actionsById.get(selectedActionId);
        if (selectedAction == null) {
            throw actionInvariantViolation();
        }
        return new GenerationContext(
                checkInId,
                selectedAction.getActionId(),
                selectedAction.getCategory(),
                selectedAction.getGuideText(),
                selectedAction.getSource());
    }

    private CareCardGeneratedText generate(GenerationContext context) {
        try {
            return careCardGenerator.generate(new CareCardGenerationRequest(
                    context.category(),
                    context.guideText()));
        } catch (CareCardGenerationException exception) {
            throw new ApiException(errorCodeFor(exception.getReason()));
        }
    }

    private CareCardCreationResult saveGeneratedCard(
            GenerationContext context,
            CareCardGeneratedText generatedText
    ) {
        CareCard existing = careCardRepository.findByCheckInId(context.checkInId()).orElse(null);
        if (existing != null) {
            return new CareCardCreationResult(toResponse(existing), false);
        }

        Action action = actionRepository.findById(context.actionId())
                .orElseThrow(this::actionInvariantViolation);
        CareCard saved = careCardRepository.saveAndFlush(new CareCard(
                generatedText.actionName(),
                generatedText.actionReason(),
                context.source(),
                context.checkInId(),
                action,
                LocalDate.now(clock)));
        return new CareCardCreationResult(toResponse(saved), true);
    }

    private CareCardResponse readExistingCard(long checkInId, long userId) {
        return readTransactions.execute(status -> {
            requireOwnedCheckIn(checkInId, userId, ErrorCode.CHECK_IN_NOT_FOUND);
            return careCardRepository.findByCheckInId(checkInId)
                    .map(this::toResponse)
                    .orElse(null);
        });
    }

    private CareCard requireOwnedCareCard(long careCardId, long userId) {
        CareCard careCard = careCardRepository.findById(careCardId)
                .orElseThrow(() -> new ApiException(ErrorCode.CARE_CARD_NOT_FOUND));
        requireOwnedCheckIn(careCard.getCheckInId(), userId, ErrorCode.CARE_CARD_NOT_FOUND);
        return careCard;
    }

    private void requireOwnedCheckIn(long checkInId, long userId, ErrorCode errorCode) {
        if (checkInRepository.findByCheckInIdAndUserId(checkInId, userId).isEmpty()) {
            throw new ApiException(errorCode);
        }
    }

    private CareCardResponse toResponse(CareCard careCard) {
        return new CareCardResponse(
                careCard.getCareCardId(),
                careCard.getCheckInId(),
                careCard.getActionName(),
                careCard.getActionReason(),
                careCard.getAction().getCategory(),
                careCard.getSource(),
                careCard.getCreatedDate());
    }

    private ApiException actionInvariantViolation() {
        return new ApiException(ErrorCode.ACTION_INVARIANT_VIOLATION);
    }

    private ErrorCode errorCodeFor(CareCardGenerationException.Reason reason) {
        return switch (reason) {
            case UNAVAILABLE -> ErrorCode.AI_PROVIDER_UNAVAILABLE;
            case AUTHENTICATION -> ErrorCode.AI_PROVIDER_AUTHENTICATION_FAILED;
            case ACCESS_DENIED -> ErrorCode.AI_PROVIDER_ACCESS_DENIED;
            case MODEL_UNAVAILABLE -> ErrorCode.AI_MODEL_UNAVAILABLE;
            case QUOTA_EXCEEDED -> ErrorCode.AI_PROVIDER_QUOTA_EXCEEDED;
            case RATE_LIMITED -> ErrorCode.AI_PROVIDER_RATE_LIMITED;
            case REQUEST_REJECTED -> ErrorCode.AI_PROVIDER_REQUEST_REJECTED;
            case TIMEOUT -> ErrorCode.AI_GENERATION_TIMEOUT;
            case UPSTREAM -> ErrorCode.AI_PROVIDER_UPSTREAM_FAILURE;
            case INVALID_OUTPUT -> ErrorCode.AI_PROVIDER_INVALID_RESPONSE;
            case NO_MATCHING_ACTION -> ErrorCode.ACTION_INVARIANT_VIOLATION;
        };
    }

    private <T> T requiredTransactionResult(T result) {
        return Objects.requireNonNull(result, "transaction result");
    }

    private sealed interface Preparation permits ExistingCard, GenerationContext {
    }

    private record ExistingCard(CareCardResponse careCard) implements Preparation {
    }

    private record GenerationContext(
            long checkInId,
            long actionId,
            String category,
            String guideText,
            String source
    ) implements Preparation {
    }
}
