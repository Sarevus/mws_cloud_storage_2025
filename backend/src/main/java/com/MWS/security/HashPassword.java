package com.MWS.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

public class HashPassword {

    private static final int SALT_SIZE = 16;
    private static final int NUMBER_OF_ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String SEPARATOR = "#";

    public static String createPasswordHash(String plainPassword) throws Exception {
        byte[] salt = generateSalt();
        byte[] passwordHash = computeHash(plainPassword, salt);
        return formatForStorage(salt, passwordHash);
    }

    public static boolean verifyPassword(String plainPassword, String storedHash) throws Exception {

        String[] parts = storedHash.split(SEPARATOR);
        byte[] salt = decodeFromBase64(parts[0]);
        byte[] expectedHash = decodeFromBase64(parts[1]);
        byte[] actualHash = computeHash(plainPassword, salt);

        return hashesEqual(expectedHash, actualHash);
    }

    private static byte[] generateSalt() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[SALT_SIZE];
        secureRandom.nextBytes(salt);
        return salt;
    }

    private static byte[] computeHash(String password, byte[] salt) throws Exception {
        KeySpec keySpec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                NUMBER_OF_ITERATIONS,
                KEY_LENGTH
        );

        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
        return keyFactory.generateSecret(keySpec).getEncoded();
    }

    private static String formatForStorage(byte[] salt, byte[] hash) {
        String saltBase64 = encodeToBase64(salt);
        String hashBase64 = encodeToBase64(hash);
        return saltBase64 + SEPARATOR + hashBase64;
    }

    private static String encodeToBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private static byte[] decodeFromBase64(String data) {
        return Base64.getDecoder().decode(data);
    }

    private static boolean hashesEqual(byte[] expected, byte[] actual) {
        return java.util.Arrays.equals(expected, actual);
    }
}