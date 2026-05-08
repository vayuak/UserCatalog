package com.UserCatalogServiceOne.UserCatalog.Models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class User {

    public enum ContactPreference {
        EMAIL, PHONE
    }
    public enum Gender {
        MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phoneNumber;

    private String countryName;
    private String countryCode;

    @Enumerated(EnumType.STRING)
    private ContactPreference preferredContactMethod;

    private String fullName;
    private LocalDate dateOfBirth;

    @Column(columnDefinition = "TEXT")
    private String bio;
    @Enumerated(EnumType.STRING)
    private Gender gender;



    private boolean isPremium = false;

    private String profilePictureUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    private String resetToken;
    private LocalDateTime resetTokenExpiry;



    // Who follows me
    @ManyToMany
    @JoinTable(name = "follows",
            joinColumns = @JoinColumn(name = "followed_id"),
            inverseJoinColumns = @JoinColumn(name = "follower_id"))
    private Set<User> followers = new HashSet<>();

    // Who I follow
    @ManyToMany(mappedBy = "followers")
    private Set<User> following = new HashSet<>();
}