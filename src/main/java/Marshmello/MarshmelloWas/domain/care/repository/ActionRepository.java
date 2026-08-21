package Marshmello.MarshmelloWas.domain.care.repository;

import Marshmello.MarshmelloWas.domain.care.entity.Action;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionRepository extends JpaRepository<Action, Long> {

    List<Action> findByActionScoreOrderByActionIdAsc(short actionScore);
}
