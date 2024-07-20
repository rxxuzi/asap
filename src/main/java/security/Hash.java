package security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * <h1>Hash</h1>
 * The Hash class provides utility methods for generating hash values
 * of strings and files using various hashing algorithms.
 * <p>
 *
 * Currently supported algorithms are SHA-256 and MD5.
 *
 * @author rxxuzi
 */
public class Hash {

    /**
     * Generates a SHA-256 hash of the input string.
     *
     * @param input The string to hash
     * @return The SHA-256 hash of the input as a hexadecimal string
     */
    public static String sha256(String input) {
        return hash("SHA-256", input);
    }

    /**
     * Generates a SHA-256 hash of the input file.
     *
     * @param file The file to hash
     * @return The SHA-256 hash of the file contents as a hexadecimal string
     */
    public static String sha256(File file) {
        return hash("SHA-256", file);
    }

    /**
     * Generates an MD5 hash of the input string.
     *
     * @param input The string to hash
     * @return The MD5 hash of the input as a hexadecimal string
     */
    public static String md5(String input) {
        return hash("MD5", input);
    }

    /**
     * Generates an MD5 hash of the input file.
     *
     * @param file The file to hash
     * @return The MD5 hash of the file contents as a hexadecimal string
     */
    public static String md5(File file) {
        return hash("MD5", file);
    }

    /**
     * Internal method to generate a hash of a string using the specified algorithm.
     *
     * @param algorithm The hashing algorithm to use
     * @param input The string to hash
     * @return The hash of the input as a hexadecimal string, or null if an error occurs
     */
    private static String hash(String algorithm, String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] byteArray = input.getBytes();
            digest.update(byteArray);
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Internal method to generate a hash of a file using the specified algorithm.
     *
     * @param algorithm The hashing algorithm to use
     * @param file The file to hash
     * @return The hash of the file contents as a hexadecimal string, or null if an error occurs
     */
    private static String hash(String algorithm, File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            FileInputStream fis = new FileInputStream(file);
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
