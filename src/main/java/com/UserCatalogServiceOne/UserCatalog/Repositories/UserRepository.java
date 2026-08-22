package com.UserCatalogServiceOne.UserCatalog.Repositories;

import com.UserCatalogServiceOne.UserCatalog.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import java.util.Collection;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    // Exact match, case-insensitive. Use this whenever you mean "this one
    // specific user" -- see the warning on findByUsernameContainingIgnoreCase.
    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByIdentityHash(String identityHash);

    Optional<User> findByResetToken(String resetToken);

    boolean existsByUsername(String username);

    // SUBSTRING search. Correct for the user-search screen, WRONG for identity
    // lookups. UserController#getCurrentUserProfile currently uses this with
    // .findFirst() to resolve /me, which means user "bob" can be served
    // "bobby"'s profile. Switch that call to findByUsernameIgnoreCase.
    List<User> findByUsernameContainingIgnoreCase(String username);

    // Batch key fetch so an inbox resolves every peer key in one query.
    List<User> findByUsernameInIgnoreCase(Collection<String> usernames);
}
