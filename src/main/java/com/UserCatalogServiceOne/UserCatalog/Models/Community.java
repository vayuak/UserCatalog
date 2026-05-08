package com.UserCatalogServiceOne.UserCatalog.Models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "communities")
@Data
public class Community {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private Integer creatorId;

    @ElementCollection
    private Set<Integer> members = new HashSet<>();
}
