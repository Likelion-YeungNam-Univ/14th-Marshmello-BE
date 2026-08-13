package Marshmello.MarshmelloWas.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "social_account",
        indexes = @Index(name = "idx_social_account_user_id", columnList = "user_id")
)
public class SocialAccount {

    @EmbeddedId
    private SocialAccountId socialAccountId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    protected SocialAccount() {
    }

    public SocialAccount(SocialAccountId socialAccountId, Long userId) {
        this.socialAccountId = socialAccountId;
        this.userId = userId;
    }

    public SocialAccountId getSocialAccountId() {
        return socialAccountId;
    }

    public Long getUserId() {
        return userId;
    }
}
