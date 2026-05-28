package iuh.fit.devhub_be.auth.model;

import com.github.f4b6a3.uuid.UuidCreator;
import iuh.fit.devhub_be.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class User extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id = UuidCreator.getTimeOrdered();

    @Column(nullable = false, updatable = false, unique = true)
    private String email;

    private String userName;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}
