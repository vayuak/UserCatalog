package com.UserCatalogServiceOne.UserCatalog.Repositories;



import com.UserCatalogServiceOne.UserCatalog.Models.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {
    boolean existsByName(String name);
}
