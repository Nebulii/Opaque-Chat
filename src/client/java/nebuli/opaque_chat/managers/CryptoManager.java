package nebuli.opaque_chat.managers;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

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

            return new String[]{ base64Public, base64Private };

        } catch (Exception e) {
            System.err.println("[Opaque Chat] Failed to generate ECC keys!");
            e.printStackTrace();
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
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(sharedSecret, 0, 16, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] encryptedText = cipher.doFinal(plainText.getBytes());

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
        byte[] plainText = cipher.doFinal(encryptedText);

        return new String(plainText);
    }
}