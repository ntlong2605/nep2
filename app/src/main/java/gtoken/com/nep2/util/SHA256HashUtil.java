package gtoken.com.nep2.util;

/**
 * Created by furyvn on 3/6/18.
 */

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public final class SHA256HashUtil {

    /**
     * the constructor.
     */
    private SHA256HashUtil() {

    }

    /**
     * returns the sha256 hash of the sha256 hash of the bytes. (it calls the has
     * function twice, passing the output of the first call as the input to the
     * second call.)
     *
     * @param bytes the bytes to hash.
     * @return the hash.
     */
    public static byte[] getDoubleSHA256Hash(final byte[] bytes) {
        return getSHA256Hash(getSHA256Hash(bytes));
    }

    /**
     * returns the sha256 hash of the bytes.
     *
     * @param bytes the bytes to hash.
     * @return the hash.
     */
    public static byte[] getSHA256Hash(final byte[] bytes) {
        final String digestName = "SHA-256";
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(digestName);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("exception getting MessageDigest \"" + digestName + "\"", e);
        }
        final byte[] hash = digest.digest(bytes);
        return hash;
    }
}
