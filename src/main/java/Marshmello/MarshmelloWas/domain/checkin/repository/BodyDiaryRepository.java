package Marshmello.MarshmelloWas.domain.checkin.repository;

import Marshmello.MarshmelloWas.domain.checkin.entity.BodyDiary;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.entity.BodyDiaryId;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BodyDiaryRepository extends JpaRepository<BodyDiary, BodyDiaryId> {

    List<BodyDiary> findByBodyDiaryIdCheckInIdInOrderByBodyDiaryIdBodyRegionAsc(Collection<Long> checkInIds);
}
