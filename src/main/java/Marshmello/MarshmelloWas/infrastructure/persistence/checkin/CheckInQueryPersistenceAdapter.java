package Marshmello.MarshmelloWas.infrastructure.persistence.checkin;

import Marshmello.MarshmelloWas.domain.analysis.entity.ImageAnalysis;
import Marshmello.MarshmelloWas.domain.analysis.repository.ImageAnalysisRepository;
import Marshmello.MarshmelloWas.domain.checkin.entity.BodyDiary;
import Marshmello.MarshmelloWas.domain.checkin.repository.BodyDiaryRepository;
import Marshmello.MarshmelloWas.domain.checkin.dto.BodyDiaryView;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInImageReference;
import Marshmello.MarshmelloWas.domain.checkin.model.CheckInPage;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInPageRequest;
import Marshmello.MarshmelloWas.domain.checkin.port.CheckInQueryPort;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInSummaryView;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInTrendView;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInView;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CheckInQueryPersistenceAdapter implements CheckInQueryPort {

    private final CheckInRepository checkInRepository;
    private final ImageRepository imageRepository;
    private final BodyDiaryRepository bodyDiaryRepository;
    private final ImageAnalysisRepository imageAnalysisRepository;

    public CheckInQueryPersistenceAdapter(
            CheckInRepository checkInRepository,
            ImageRepository imageRepository,
            BodyDiaryRepository bodyDiaryRepository,
            ImageAnalysisRepository imageAnalysisRepository
    ) {
        this.checkInRepository = checkInRepository;
        this.imageRepository = imageRepository;
        this.bodyDiaryRepository = bodyDiaryRepository;
        this.imageAnalysisRepository = imageAnalysisRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CheckInView> findOwnedById(long checkInId, long userId) {
        return checkInRepository.findByCheckInIdAndUserId(checkInId, userId)
                .map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckInPage<CheckInSummaryView> findPageByUserId(long userId, CheckInPageRequest pageRequest) {
        Pageable fixedOrder = PageRequest.of(
                pageRequest.page(),
                pageRequest.size(),
                Sort.by(Sort.Order.desc("checkInDate"), Sort.Order.desc("checkInId"))
        );
        Page<CheckIn> checkIns = checkInRepository.findByUserId(userId, fixedOrder);
        Map<Long, Short> scores = scoresByCheckInId(checkIns.getContent().stream()
                .map(CheckIn::getCheckInId)
                .toList());
        List<CheckInSummaryView> content = checkIns.getContent().stream()
                .map(checkIn -> new CheckInSummaryView(
                        checkIn.getCheckInId(),
                        checkIn.isAchieved(),
                        checkIn.getCheckInDate(),
                        requiredScore(scores, checkIn.getCheckInId())
                ))
                .toList();
        return new CheckInPage<>(content, checkIns.getTotalElements(), checkIns.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckInTrendView> findTrendsByUserIdBetween(long userId, LocalDate periodStart, LocalDate periodEnd) {
        List<CheckIn> checkIns = checkInRepository.findByUserIdAndCheckInDateBetweenOrderByCheckInDateAscCheckInIdAsc(
                userId,
                periodStart,
                periodEnd
        );
        Map<Long, Short> scores = scoresByCheckInId(checkIns.stream().map(CheckIn::getCheckInId).toList());
        return checkIns.stream()
                .map(checkIn -> new CheckInTrendView(
                        checkIn.getCheckInDate(),
                        requiredScore(scores, checkIn.getCheckInId()),
                        checkIn.isAchieved()
                ))
                .toList();
    }

    private CheckInView toView(CheckIn checkIn) {
        Long imageId = imageIdsByCheckInId(List.of(checkIn.getCheckInId())).get(checkIn.getCheckInId());
        if (imageId == null) {
            throw new IllegalStateException("Check-in image is missing");
        }
        ImageAnalysis analysis = imageAnalysisRepository.findById(imageId)
                .orElseThrow(() -> new IllegalStateException("Image analysis is missing"));
        List<BodyDiaryView> bodyDiaries = bodyDiaryRepository
                .findByBodyDiaryIdCheckInIdInOrderByBodyDiaryIdBodyRegionAsc(List.of(checkIn.getCheckInId()))
                .stream()
                .map(bodyDiary -> new BodyDiaryView(
                        bodyDiary.getBodyDiaryId().getBodyRegion(),
                        bodyDiary.getStretchMark(),
                        bodyDiary.getComment()
                ))
                .toList();
        return new CheckInView(
                checkIn.getCheckInId(),
                checkIn.isAchieved(),
                checkIn.getCheckInDate(),
                checkIn.getDiary(),
                checkIn.getEmotion(),
                analysis.getScore(),
                bodyDiaries
        );
    }

    private Map<Long, Short> scoresByCheckInId(Collection<Long> checkInIds) {
        Map<Long, Long> imageIdsByCheckInId = imageIdsByCheckInId(checkInIds);
        Map<Long, ImageAnalysis> analysesByImageId = imageAnalysisRepository.findByImageIdIn(imageIdsByCheckInId.values())
                .stream()
                .collect(Collectors.toMap(ImageAnalysis::getImageId, Function.identity()));
        return imageIdsByCheckInId.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> requiredAnalysis(analysesByImageId, entry.getValue()).getScore()
        ));
    }

    private ImageAnalysis requiredAnalysis(Map<Long, ImageAnalysis> analysesByImageId, Long imageId) {
        ImageAnalysis analysis = analysesByImageId.get(imageId);
        if (analysis == null) {
            throw new IllegalStateException("Check-in analysis is missing");
        }
        return analysis;
    }

    private Map<Long, Long> imageIdsByCheckInId(Collection<Long> checkInIds) {
        return imageRepository.findReferencesByCheckInCheckInIdIn(checkInIds).stream().collect(Collectors.toMap(
                CheckInImageReference::checkInId,
                CheckInImageReference::imageId
        ));
    }

    private short requiredScore(Map<Long, Short> scores, Long checkInId) {
        Short score = scores.get(checkInId);
        if (score == null) {
            throw new IllegalStateException("Check-in analysis is missing");
        }
        return score;
    }
}
