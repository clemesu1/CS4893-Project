package com.clemesu1.network.client;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

/**
 * https://howtodoinjava.com/java/java-security/aes-256-encryption-decryption/
 */
public class AES {

    private static SecretKeySpec secretKey;
    private static byte[] key;

    /**
     * Encrypt a ciphered message to be sent over the server.
     * @param message Message to be encrypted.
     * @param secret Secret key.
     * @return Encrypted message or null.
     */
    public static String encrypt(String message, String secret) {
        try {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] cipherText = cipher.doFinal(message.getBytes());
            return Base64.getEncoder().encodeToString(cipherText);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | BadPaddingException | IllegalBlockSizeException e) {
            System.err.println("Error while encrypting: " + e);
        }
        return null;
    }

    /**
     * Decrypt a ciphered message received from the socket.
     * @param cipherText Message to be decrypted.
     * @param secret Secret key.
     * @return Decrypted message or null.
     */
    public static String decrypt(String cipherText, String secret) {
        try {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] plainText = cipher.doFinal(Base64.getMimeDecoder().decode(cipherText));
            return new String(plainText);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                | BadPaddingException | IllegalBlockSizeException e) {
            System.err.println("Error while encrypting: " + e);
        }
        return null;
    }

    /**
     * Set the key for the cipher.
     * @param keyString key value.
     */
    private static void setKey(String keyString) {
        try {
            key = keyString.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
