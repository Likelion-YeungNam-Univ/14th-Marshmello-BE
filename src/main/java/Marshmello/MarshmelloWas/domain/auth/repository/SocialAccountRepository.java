package Marshmello.MarshmelloWas.domain.auth.repository;

import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccount;
import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccountId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, SocialAccountId> {

    List<SocialAccount> findByUserId(Long userId);
}
