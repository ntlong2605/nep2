package gtoken.com.nep2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import gtoken.com.nep2.scrypt.crypto.SCryptUtil;
import gtoken.com.nep2.util.Base58;
import gtoken.com.nep2.util.SHA256HashUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    //Data just for testing
    private static final String mRawPrivateKey = "09C2686880095B1A4C249EE3AC4EEA8A014F11E6F986D0B5025AC1F39AFBD9AE";
    private static final String mAddress = "AXoxAX2eJfJ1shNpWqUxRh3RWNUJqvQvVa";
    private static final String mPassphrase = "Satoshi";
    private static final String mEncryptedNEP2 = "6PYN6mjwYfjPUuYT3Exajvx25UddFVLpCw4bMsmtLdnKwZ9t1Mi3CfKe8S";

    private static final int N = 16384;
    private static final int r = 8;
    private static final int p = 8;
    private static final int length = 64;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //encrypt();

        decrypt(mPassphrase, mEncryptedNEP2);
    }

    public byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }


    private void encrypt() {
        /*
         * Step 1: Compute the NEO address (ASCII), and take the first four bytes of SHA256(SHA256()) of it. Let's call this "addresshash".
         */
        byte[] doubleSHA256Hash = SHA256HashUtil.getDoubleSHA256Hash(mAddress.getBytes());
        Log.d(TAG, "doubleSHA256Hash=" + Arrays.toString(doubleSHA256Hash));

        //get the first 4 bytes
        byte[] salt = new byte[4];
        System.arraycopy(doubleSHA256Hash, 0, salt, 0, 4);
        Log.d(TAG, "salt=" + Arrays.toString(salt));

        /*
         * Step 2: Derive a key from the passphrase using Scrypt
         */
        byte[] scryptKey = SCryptUtil.scrypt(mPassphrase, salt, N, r, p);
        Log.d(TAG, "scryptKey=" + Arrays.toString(scryptKey) + " ,length=" + scryptKey.length);

        //Split derive key into 2 parts
        byte[] derivedhalf1 = Arrays.copyOfRange(scryptKey, 0, 32);
        byte[] derivedhalf2 = Arrays.copyOfRange(scryptKey, 32, 64);
        Log.d(TAG, "derivedhalf1=" + Arrays.toString(derivedhalf1) + " ,length=" + derivedhalf1.length);
        Log.d(TAG, "derivedhalf2=" + Arrays.toString(derivedhalf2) + " ,length=" + derivedhalf2.length);

        /*
         * Step 3: out encryptedkey
         */
        byte[] privateKeyByteArray = hexStringToByteArray(mRawPrivateKey);
        Log.d(TAG, "privateKeyByteArray=" + Arrays.toString(privateKeyByteArray) + " ,length=" + privateKeyByteArray.length);

        //Do AES256 encrypt
        byte[] xor = new byte[32];
        for (int i = 0; i < 32; i++) {
            xor[i] = (byte) (privateKeyByteArray[i] ^ derivedhalf1[i]);
        }
        byte[] encryptedkey = doAES256Encrypt(xor, derivedhalf2);
        Log.d(TAG, "encryptedkey=" + Arrays.toString(encryptedkey) + " ,length=" + encryptedkey.length);

        //Combine into one: 0x01 0x42 + flagbyte + salt + encryptedkey
        byte[] prefix = hexStringToByteArray("0142");
        byte[] flagbyte = hexStringToByteArray("E0");
        ByteBuffer bb = ByteBuffer.allocate(prefix.length + flagbyte.length + salt.length + encryptedkey.length);
        bb.put(prefix);
        bb.put(flagbyte);
        bb.put(salt);
        bb.put(encryptedkey);
        byte[] payload = bb.array();

        //Add checksum
        byte[] payloadDoubleHash = SHA256HashUtil.getDoubleSHA256Hash(payload);
        byte[] checkSum = new byte[4];
        System.arraycopy(payloadDoubleHash, 0, checkSum, 0, checkSum.length);
        ByteBuffer bb2 = ByteBuffer.allocate(payload.length + checkSum.length);
        bb2.put(payload);
        bb2.put(checkSum);
        byte[] encryptedPrivateKey = bb2.array();

        Log.d(TAG, "encryptedPrivateKey=" + Arrays.toString(encryptedPrivateKey) + " ,length=" + encryptedPrivateKey.length);
        String base58 = Base58.encode(encryptedPrivateKey);
        Log.d(TAG, "Base58 encryptedPrivateKey=" + base58 + " ,length=" + base58.length());
    }

    private void decrypt(String passPhrase, String encryptedNEP2) {

        //decrypt payload
        byte[] decryptedPayload = Base58.decode(encryptedNEP2);

        //decrypt salt
        byte[] salt = Arrays.copyOfRange(decryptedPayload, 3, 7);
        Log.d(TAG, "salt=" + Arrays.toString(salt) + " ,length=" + salt.length);

        //decrypt encrypted private key
        byte[] encryptedPrivateKey = Arrays.copyOfRange(decryptedPayload, 7, 39);
        Log.d(TAG, "encryptedPrivateKey=" + Arrays.toString(encryptedPrivateKey) + " ,length=" + encryptedPrivateKey.length);

        //Devive Scrypt key
        byte[] scryptKey = SCryptUtil.scrypt(mPassphrase, salt, N, r, p);
        Log.d(TAG, "scryptKey=" + Arrays.toString(scryptKey) + "length=" + scryptKey.length);

        //Split derive key into 2 parts
        byte[] derivedhalf1 = Arrays.copyOfRange(scryptKey, 0, 32);
        byte[] derivedhalf2 = Arrays.copyOfRange(scryptKey, 32, 64);
        Log.d(TAG, "derivedhalf1=" + Arrays.toString(derivedhalf1) + " ,length=" + derivedhalf1.length);
        Log.d(TAG, "derivedhalf2=" + Arrays.toString(derivedhalf2) + " ,length=" + derivedhalf2.length);

        //DecryptSHA256 to get XOR
        byte[] xor = doAES256Decrypt(encryptedPrivateKey, derivedhalf2);

        //form private key
        byte[] privateKeyByteArray = doXor(xor, derivedhalf1);

        Log.d(TAG, "raw private key=" + bytesToHex(privateKeyByteArray));

    }

    private byte[] doAES256Encrypt(byte[] input, byte[] key) {
        byte[] output = null;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            output = cipher.doFinal(input);
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return output;
    }

    private byte[] doAES256Decrypt(byte[] input, byte[] key) {
        byte[] output = null;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            output = cipher.doFinal(input);
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return output;
    }

    private byte[] doXor(byte[] data1, byte[] data2) {
        // make data2 the largest...
        if (data1.length > data2.length) {
            byte[] tmp = data2;
            data2 = data1;
            data1 = tmp;
        }
        for (int i = 0; i < data1.length; i++) {
            data2[i] ^= data1[i];
        }
        return data2;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


}
