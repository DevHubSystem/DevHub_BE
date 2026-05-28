package iuh.fit.devhub_be.auth.model;

import com.github.f4b6a3.uuid.UuidCreator;
import iuh.fit.devhub_be.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "roles")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Role extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id = UuidCreator.getTimeOrdered();

    @Column(nullable = false, unique = true)
    private String name;
}
