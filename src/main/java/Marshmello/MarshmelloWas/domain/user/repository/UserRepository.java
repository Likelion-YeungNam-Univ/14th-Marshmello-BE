package Marshmello.MarshmelloWas.domain.user.repository;

import Marshmello.MarshmelloWas.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
