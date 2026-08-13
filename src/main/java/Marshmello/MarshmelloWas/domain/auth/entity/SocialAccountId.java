package Marshmello.MarshmelloWas.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SocialAccountId implements Serializable {

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    protected SocialAccountId() {
    }

    public SocialAccountId(String provider, String providerUserId) {
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SocialAccountId that)) {
            return false;
        }
        return Objects.equals(provider, that.provider)
                && Objects.equals(providerUserId, that.providerUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, providerUserId);
    }
}
