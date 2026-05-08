package com.UserCatalogServiceOne.UserCatalog.Repositories;

import com.UserCatalogServiceOne.UserCatalog.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByResetToken(String resetToken);

    boolean existsByUsername(String username);
}