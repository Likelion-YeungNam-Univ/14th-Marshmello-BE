package Marshmello.MarshmelloWas.domain.care.repository;

import Marshmello.MarshmelloWas.domain.care.entity.UserActionFeedback;
import Marshmello.MarshmelloWas.domain.care.entity.UserActionFeedbackId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActionFeedbackRepository
        extends JpaRepository<UserActionFeedback, UserActionFeedbackId> {

    List<UserActionFeedback> findByFeedbackIdUserIdAndFeedbackIdActionIdIn(
            Long userId,
            Collection<Long> actionIds
    );
}
