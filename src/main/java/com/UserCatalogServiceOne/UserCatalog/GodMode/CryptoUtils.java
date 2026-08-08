package com.UserCatalogServiceOne.UserCatalog.GodMode;

import org.apache.commons.codec.digest.DigestUtils;

public class CryptoUtils {
    // Falls back to a deterministic local constant if environment variable is unassigned
    private static final String PEPPER = System.getenv("DB_PEPPER") != null ?
            System.getenv("DB_PEPPER") : "LocalCryptoPepperFallbackStringConstantString";

    public static String hashIdentifier(String identifier) {
        if (identifier == null) return null;
        return DigestUtils.sha256Hex(identifier.trim() + PEPPER);
    }
}