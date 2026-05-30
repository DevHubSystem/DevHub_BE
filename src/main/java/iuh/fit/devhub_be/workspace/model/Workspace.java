package iuh.fit.devhub_be.workspace.model;

import com.github.f4b6a3.uuid.UuidCreator;
import iuh.fit.devhub_be.auth.model.User;
import iuh.fit.devhub_be.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Workspace extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id = UuidCreator.getTimeOrdered();

    /**
     * Human-facing unique identifier used in URLs for CRUD instead of the UUID id.
     * Exactly 6 chars, uppercase letters + digits, at least one letter (e.g. "DEV001").
     */
    @Column(nullable = false, unique = true, length = 6)
    private String reminderKey;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "workspace_members",
            joinColumns = @JoinColumn(name = "workspace_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();
}
