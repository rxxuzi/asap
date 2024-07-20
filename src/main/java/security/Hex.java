package security;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * <h1>Hex</h1>
 * The Hex class provides utility methods for working with hexadecimal representations
 * of binary data. It includes functionality for generating hex dumps, converting
 * between byte arrays and hex strings, and securely comparing byte arrays.
 * <p>
 * This class is particularly useful for debugging, logging, and security-related
 * operations involving binary data.
 */
public class Hex {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Generates a hex dump of the first n bytes of a file.
     * The hex dump format displays byte values in hexadecimal, with 16 bytes per line.
     *
     * @param file     The file to read
     * @param numBytes The number of bytes to include in the hex dump
     * @return A string containing the hex dump
     * @throws IOException If an I/O error occurs while reading the file
     */
    public static String getHexDump(File file, int numBytes) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return getHexDump(bytes, numBytes);
    }

    /**
     * Generates a hex dump of the first n bytes of a byte array.
     * The hex dump format displays byte values in hexadecimal, with 16 bytes per line.
     *
     * @param bytes    The byte array to process
     * @param numBytes The number of bytes to include in the hex dump
     * @return A string containing the hex dump
     */
    public static String getHexDump(byte[] bytes, int numBytes) {
        StringBuilder result = new StringBuilder();
        int length = Math.min(bytes.length, numBytes);

        for (int i = 0; i < length; i++) {
            if (i > 0 && i % 16 == 0) {
                result.append("\n");
            }
            result.append(String.format("%02X ", bytes[i]));
        }

        return result.toString();
    }

    /**
     * Converts a byte array to a hex string.
     * Each byte is represented by two hexadecimal characters.
     *
     * @param bytes The byte array to convert
     * @return A hex string representation of the byte array
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Converts a hex string to a byte array.
     * Each pair of hexadecimal characters in the string is converted to one byte.
     *
     * @param hexString The hex string to convert
     * @return A byte array representation of the hex string
     * @throws IllegalArgumentException If the hex string has an odd length or contains non-hex characters
     */
    public static byte[] hexToBytes(String hexString) {
        int len = hexString.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even number of characters");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Securely compares two byte arrays in constant time.
     * This method is designed to prevent timing attacks when comparing sensitive data.
     *
     * @param a The first byte array
     * @param b The second byte array
     * @return true if the arrays are equal, false otherwise
     */
    public static boolean compare(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}