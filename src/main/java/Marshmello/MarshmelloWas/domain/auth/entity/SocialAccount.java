package Marshmello.MarshmelloWas.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

@Entity
@Table(
        name = "social_account",
        indexes = @Index(name = "idx_social_account_user_id", columnList = "user_id")
)
public class SocialAccount implements Persistable<SocialAccountId> {

    @EmbeddedId
    private SocialAccountId socialAccountId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Transient
    private boolean newEntity = true;

    protected SocialAccount() {
    }

    public SocialAccount(SocialAccountId socialAccountId, Long userId) {
        this.socialAccountId = socialAccountId;
        this.userId = userId;
    }

    public SocialAccountId getSocialAccountId() {
        return socialAccountId;
    }

    @Override
    public SocialAccountId getId() {
        return socialAccountId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        newEntity = false;
    }

    public Long getUserId() {
        return userId;
    }
}
