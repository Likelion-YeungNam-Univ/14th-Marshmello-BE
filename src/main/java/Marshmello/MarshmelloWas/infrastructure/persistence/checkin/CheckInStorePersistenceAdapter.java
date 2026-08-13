package Marshmello.MarshmelloWas.infrastructure.persistence.checkin;

import Marshmello.MarshmelloWas.domain.analysis.entity.ImageAnalysis;
import Marshmello.MarshmelloWas.domain.analysis.repository.ImageAnalysisRepository;
import Marshmello.MarshmelloWas.domain.checkin.entity.BodyDiary;
import Marshmello.MarshmelloWas.domain.checkin.dto.BodyDiaryCommand;
import Marshmello.MarshmelloWas.domain.checkin.model.BodyDiaryId;
import Marshmello.MarshmelloWas.domain.checkin.repository.BodyDiaryRepository;
import Marshmello.MarshmelloWas.domain.checkin.dto.BodyDiaryView;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInCommand;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.port.CheckInStorePort;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInView;
import Marshmello.MarshmelloWas.domain.checkin.entity.Image;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CheckInStorePersistenceAdapter implements CheckInStorePort {

    private final CheckInRepository checkInRepository;
    private final ImageRepository imageRepository;
    private final BodyDiaryRepository bodyDiaryRepository;
    private final ImageAnalysisRepository imageAnalysisRepository;

    public CheckInStorePersistenceAdapter(
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
    @Transactional
    public CheckInView save(CheckInCommand command, short score) {
        CheckIn checkIn = checkInRepository.save(new CheckIn(
                command.achieved(),
                command.checkInDate(),
                command.diary(),
                command.emotion(),
                command.userId()
        ));
        Image image = imageRepository.save(new Image(command.imageData(), checkIn));
        List<BodyDiaryView> bodyDiaries = command.bodyDiaries().stream()
                .map(bodyDiaryCommand -> saveBodyDiary(bodyDiaryCommand, checkIn))
                .toList();
        imageAnalysisRepository.save(new ImageAnalysis(image.getImageId(), score));

        return new CheckInView(
                checkIn.getCheckInId(),
                checkIn.isAchieved(),
                checkIn.getCheckInDate(),
                checkIn.getDiary(),
                checkIn.getEmotion(),
                score,
                bodyDiaries
        );
    }

    private BodyDiaryView saveBodyDiary(BodyDiaryCommand command, CheckIn checkIn) {
        BodyDiary bodyDiary = bodyDiaryRepository.save(new BodyDiary(
                new BodyDiaryId(command.bodyRegion(), checkIn.getCheckInId()),
                checkIn,
                command.stretchMark(),
                command.comment()
        ));
        return new BodyDiaryView(
                bodyDiary.getBodyDiaryId().getBodyRegion(),
                bodyDiary.getStretchMark(),
                bodyDiary.getComment()
        );
    }
}
