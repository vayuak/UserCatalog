package com.UserCatalogServiceOne.UserCatalog.Repositories;

import com.UserCatalogServiceOne.UserCatalog.Models.Post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Fetch feed by city and category (e.g., Search "Delhi" + "SCAM_ALERT")
    Page<Post> findByCityNameAndCategoryOrderByCreatedAtDesc(
            String cityName, String category, Pageable pageable);

    // Fetch general feed for a city
    Page<Post> findByCityNameOrderByCreatedAtDesc(String cityName, Pageable pageable);
}