package eu.cloudnetservice.cloudnet.repository.github.webhook;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class GitHubWebHookAuthenticator {

    public static final String SIGNATURE_PREFIX = "sha1=";

    public static byte[] getExpectedSignature(String secret, byte[] body) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(), "HmacSHA1");
        Mac hmac = Mac.getInstance("HmacSHA1");
        hmac.init(key);
        return hmac.doFinal(body);
    }

    public static boolean validateSignature(String signatureHeader, String secret, byte[] body) throws DecoderException, IllegalArgumentException, InvalidKeyException, NoSuchAlgorithmException {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            throw new IllegalArgumentException("Unsupported webhook signature type: " + signatureHeader);
        }

        byte[] signature = Hex.decodeHex(signatureHeader.substring(SIGNATURE_PREFIX.length()).toCharArray());
        return MessageDigest.isEqual(signature, getExpectedSignature(secret, body));
    }

}
