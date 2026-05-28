package iuh.fit.devhub_be.auth.model;

import com.github.f4b6a3.uuid.UuidCreator;
import iuh.fit.devhub_be.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class RefreshToken extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id = UuidCreator.getTimeOrdered();

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;
}
