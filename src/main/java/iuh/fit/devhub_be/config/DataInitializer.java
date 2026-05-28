package iuh.fit.devhub_be.config;

import iuh.fit.devhub_be.auth.model.Role;
import iuh.fit.devhub_be.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roleRepository.findByName("USER").isEmpty()) {
            Role role = new Role();
            role.setName("USER");
            roleRepository.save(role);
        }
    }
}
