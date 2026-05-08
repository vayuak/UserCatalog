package com.UserCatalogServiceOne.UserCatalog.GodMode;

public class CryptoUtils {
    // This "Pepper" should be an Environment Variable, NEVER in code.
    private static final String PEPPER = System.getenv("DB_PEPPER");

    public static String hashIdentifier(String identifier) {
        if (identifier == null) return null;
        // SHA-256 + Pepper makes it impossible to reverse-lookup
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(identifier + PEPPER);
    }
}
