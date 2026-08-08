package com.UserCatalogServiceOne.UserCatalog.Repositories;

import com.UserCatalogServiceOne.UserCatalog.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByIdentityHash(String identityHash);

    Optional<User> findByResetToken(String resetToken);

    boolean existsByUsername(String username);

    // 🟢 FIXED: Added signature so UserController compiles seamlessly
    List<User> findByUsernameContainingIgnoreCase(String username);
}