package com.UserCatalogServiceOne.UserCatalog.Models;

import jakarta.persistence.*;
import lombok.Data;

import javax.xml.stream.events.Comment;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity


@Table(name = "posts")
@Data
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer userId;
    private String mediaId;
    private String caption;

    // Ground Reality Fields
    private String cityName;    // e.g., "Paris", "Delhi"
    private String category;    // SCAM_ALERT, BUDGET_STAY, TRANSPORT, TIPS
    private LocalDateTime createdAt = LocalDateTime.now();

    @ElementCollection
    private Set<Integer> likes = new HashSet<>();
}