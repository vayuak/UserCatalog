package com.UserCatalogServiceOne.UserCatalog.Models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class BudgetStay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String cityName;
    private String type; // e.g., "Government Hostel", "Dormitory"
    private Double pricePerNight;
    private boolean verifiedByLocal; // True if a local human confirmed it's real
}
