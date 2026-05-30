package iuh.fit.devhub_be.workspace.repository;

import iuh.fit.devhub_be.auth.model.User;
import iuh.fit.devhub_be.workspace.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    /**
     * Workspaces the user owns or is a member of, de-duplicated.
     */
    @Query("SELECT DISTINCT w FROM Workspace w LEFT JOIN w.members m WHERE w.owner = :user OR m = :user")
    List<Workspace> findAllByOwnerOrMember(@Param("user") User user);

    /**
     * Looks up a workspace by its public reminder key (the URL identifier).
     */
    Optional<Workspace> findByReminderKey(String reminderKey);

    /**
     * Whether a workspace already uses the given reminder key (uniqueness guard on create).
     */
    boolean existsByReminderKey(String reminderKey);
}
