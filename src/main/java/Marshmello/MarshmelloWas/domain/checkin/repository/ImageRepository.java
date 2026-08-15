package Marshmello.MarshmelloWas.domain.checkin.repository;

import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInImageReference;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.entity.Image;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface ImageRepository extends JpaRepository<Image, Long> {

    @Query("""
            select new Marshmello.MarshmelloWas.domain.checkin.dto.CheckInImageReference(
                image.checkIn.checkInId,
                image.imageId
            )
            from Image image
            where image.checkIn.checkInId = :checkInId
            """)
    Optional<CheckInImageReference> findReferenceByCheckInId(Long checkInId);

    @Query("""
            select new Marshmello.MarshmelloWas.domain.checkin.dto.CheckInImageReference(
                image.checkIn.checkInId,
                image.imageId
            )
            from Image image
            where image.checkIn.checkInId in :checkInIds
            """)
    List<CheckInImageReference> findReferencesByCheckInCheckInIdIn(Collection<Long> checkInIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select image from Image image where image.imageId = :imageId")
    Optional<Image> findByIdForUpdate(Long imageId);

    Optional<Image> findByImageIdAndUserIdAndCheckInIsNotNull(Long imageId, Long userId);
}
