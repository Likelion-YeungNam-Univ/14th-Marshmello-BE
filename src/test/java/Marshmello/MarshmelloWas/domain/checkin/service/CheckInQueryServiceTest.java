package Marshmello.MarshmelloWas.domain.checkin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInImageReference;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInPageRequest;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.repository.BodyDiaryRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageAnalysisRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class CheckInQueryServiceTest {

    @Test
    void reportsMissingAnalysisWithAnExplicitInvariantFailure() {
        CheckInRepository checkInRepository = mock(CheckInRepository.class);
        ImageRepository imageRepository = mock(ImageRepository.class);
        BodyDiaryRepository bodyDiaryRepository = mock(BodyDiaryRepository.class);
        ImageAnalysisRepository imageAnalysisRepository = mock(ImageAnalysisRepository.class);
        CheckIn checkIn = new CheckIn(false, LocalDate.of(2026, 8, 13), null, (short) 1, 7L);
        ReflectionTestUtils.setField(checkIn, "checkInId", 11L);

        when(checkInRepository.findByUserId(eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(checkIn)));
        when(imageRepository.findReferencesByCheckInCheckInIdIn(List.of(11L)))
                .thenReturn(List.of(new CheckInImageReference(11L, 21L)));
        when(imageAnalysisRepository.findByImageIdIn(List.of(21L))).thenReturn(List.of());

        CheckInQueryService service = new CheckInQueryService(
                checkInRepository,
                imageRepository,
                bodyDiaryRepository,
                imageAnalysisRepository
        );

        assertThatThrownBy(() -> service.findPageByUserId(7L, new CheckInPageRequest(0, 10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Check-in analysis is missing");
    }
}
