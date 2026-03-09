package nebuli.opaque_chat.managers;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CryptoManager {
    public static String[] generateKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");

            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
            keyGen.initialize(ecSpec);

            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();

            String base64Private = Base64.getEncoder().encodeToString(privateKey.getEncoded());
            String base64Public = Base64.getEncoder().encodeToString(publicKey.getEncoded());

            return new String[]{base64Public, base64Private};

        } catch (Exception e) {
            System.err.println("[Opaque Chat] Failed to generate ECC keys!");
            return null;
        }
    }

    public static PrivateKey getPrivateKeyFromString(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePrivate(spec);
    }

    public static PublicKey getPublicKeyFromString(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePublic(spec);
    }

    public static byte[] getSharedSecret(String myPrivateBase64, String theirPublicBase64) throws Exception {
        PrivateKey myPrivate = getPrivateKeyFromString(myPrivateBase64);
        PublicKey theirPublic = getPublicKeyFromString(theirPublicBase64);

        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(myPrivate);
        keyAgreement.doPhase(theirPublic, true);

        return keyAgreement.generateSecret();
    }

    public static String encryptMessage(String plainText, byte[] sharedSecret) throws Exception {
        byte[] plainBytes = plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        byte[] compressedBytes = compress(plainBytes);

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(sharedSecret, 0, 16, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        byte[] encryptedText = cipher.doFinal(compressedBytes);

        byte[] payload = new byte[iv.length + encryptedText.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(encryptedText, 0, payload, iv.length, encryptedText.length);

        return Base64.getEncoder().encodeToString(payload);
    }

    public static String decryptMessage(String base64Payload, byte[] sharedSecret) throws Exception {
        byte[] payload = Base64.getDecoder().decode(base64Payload);

        byte[] iv = new byte[12];
        System.arraycopy(payload, 0, iv, 0, 12);

        byte[] encryptedText = new byte[payload.length - 12];
        System.arraycopy(payload, 12, encryptedText, 0, encryptedText.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(sharedSecret, 0, 16, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        byte[] compressedBytes = cipher.doFinal(encryptedText);

        byte[] plainBytes = decompress(compressedBytes);

        return new String(plainBytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    public static byte[] compress(byte[] data) throws Exception {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        return outputStream.toByteArray();
    }

    public static byte[] decompress(byte[] data) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        return outputStream.toByteArray();
    }

    public static List<String> chunkMessageAtSpaces(String message, int maxPlaintextLength) {
        List<String> chunks = new java.util.ArrayList<>();
        String[] words = message.split(" ");
        StringBuilder currentChunk = new StringBuilder();

        for (String word : words) {
            if (word.length() > maxPlaintextLength) {
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk.setLength(0);
                }
                int index = 0;
                while (index < word.length()) {
                    int end = Math.min(index + maxPlaintextLength, word.length());
                    chunks.add(word.substring(index, end));
                    index = end;
                }
                continue;
            }

            if (currentChunk.length() + word.length() + 1 > maxPlaintextLength) {
                chunks.add(currentChunk.toString().trim());
                currentChunk.setLength(0);
            }
            currentChunk.append(word).append(" ");
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }
}