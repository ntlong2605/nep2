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

import gtoken.com.nep2.Util.Base58;
import gtoken.com.nep2.Util.SHA256HashUtil;
import gtoken.com.nep2.Scrypt.crypto.SCryptUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String mRawPrivateKey = "09C2686880095B1A4C249EE3AC4EEA8A014F11E6F986D0B5025AC1F39AFBD9AE";
    private static final String mAddress = "AHfQH5Ev6QP4R9MfqYU42vZs5Z7giAPnf4";
    private static final String mPassphrase = "NeoIsGood";

    private static final int N = 16384;
    private static final int r = 8;
    private static final int p = 8;
    private static final int length = 64;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        encrypt();
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
         * Step 2: Derive a key from the passphrase using scrypt
         */
        byte[] scryptKey = SCryptUtil.scrypt(mPassphrase, salt, N, r, p);
        Log.d(TAG, "scryptKey=" + Arrays.toString(scryptKey) + "length=" + scryptKey.length);

        //Split derive key into 2 parts
        byte[] derivedhalf1 = Arrays.copyOfRange(scryptKey, 0, 32);
        byte[] derivedhalf2 = Arrays.copyOfRange(scryptKey, 32, 64);

        Log.d(TAG, "derivedhalf1=" + Arrays.toString(derivedhalf1) + " ,length=" + derivedhalf1.length);
        Log.d(TAG, "derivedhalf2=" + Arrays.toString(derivedhalf2) + " ,length=" + derivedhalf2.length);

        /*
         * Step 3: out encryptedhalf1, encryptedhalf2
         */

        //Split private key into 2 parts privkey[0...15] and privkey[16...31]
        byte[] privateKeyByteArray = hexStringToByteArray(mRawPrivateKey);
        Log.d(TAG, "privateKeyByteArray=" + Arrays.toString(privateKeyByteArray) + " ,length=" + privateKeyByteArray.length);

        byte[] privkey1 = Arrays.copyOfRange(privateKeyByteArray, 0, 16);
        byte[] privkey2 = Arrays.copyOfRange(privateKeyByteArray, 16, 32);
        Log.d(TAG, "privkey1=" + Arrays.toString(privkey1) + " ,length=" + privkey1.length);
        Log.d(TAG, "privkey2=" + Arrays.toString(privkey2) + " ,length=" + privkey2.length);

        //Split derivedhalf1 into 2 parts
        byte[] derivedhalf1_1 = Arrays.copyOfRange(derivedhalf1, 0, 16);
        byte[] derivedhalf1_2 = Arrays.copyOfRange(derivedhalf1, 16, 32);

        //XOR 1: privkey[0...15] xor derivedhalf1[0...15]
        byte[] xor1 = new byte[16];
        for (int i = 0; i < 16; i++) {
            xor1[i] = (byte) (privkey1[i] ^ derivedhalf1_1[i]);
        }
        Log.d(TAG, "xor1=" + Arrays.toString(xor1) + " ,length=" + xor1.length);

        //XOR 2: privkey[16...31] xor derivedhalf1[16...31]
        byte[] xor2 = new byte[16];
        for (int i = 0; i < 16; i++) {
            xor2[i] = (byte) (privkey2[i] ^ derivedhalf1_2[i]);
        }
        Log.d(TAG, "xor2=" + Arrays.toString(xor2) + " ,length=" + xor2.length);

        //Do AES256 encrypt
        byte[] encryptedhalf1 = doAES256Encrypt(xor1, derivedhalf2);
        Log.d(TAG, "encryptedhalf1=" + Arrays.toString(encryptedhalf1) + " ,length=" + encryptedhalf1.length);
        byte[] encryptedhalf2 = doAES256Encrypt(xor2, derivedhalf2);
        Log.d(TAG, "encryptedhalf2=" + Arrays.toString(encryptedhalf2) + " ,length=" + encryptedhalf2.length);

        //Combine into one: 0x01 0x42 + flagbyte + salt + encryptedhalf1 + encryptedhalf2
        byte[] prefix = hexStringToByteArray("0142");
        byte[] flagbyte = hexStringToByteArray("E0");
        ByteBuffer bb = ByteBuffer.allocate(prefix.length + flagbyte.length + salt.length + encryptedhalf1.length + encryptedhalf2.length);
        bb.put(prefix);
        bb.put(flagbyte);
        bb.put(salt);
        bb.put(encryptedhalf1);
        bb.put(encryptedhalf2);
        byte[] encryptedPrivateKey = bb.array();

        Log.d(TAG, "encryptedPrivateKey=" + Arrays.toString(encryptedPrivateKey) + " ,length=" + encryptedPrivateKey.length);
        Log.d(TAG, "Base58 encryptedPrivateKey=" + Base58.encode(encryptedPrivateKey));


    }

    private void decrypt() {
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
         * Step 2: Derive a key from the passphrase using scrypt
         */
        byte[] scryptKey = SCryptUtil.scrypt(mPassphrase, salt, N, r, p);
        Log.d(TAG, "scryptKey=" + Arrays.toString(scryptKey) + "length=" + scryptKey.length);

        //Split derive key into 2 parts
        byte[] derivedhalf1 = Arrays.copyOfRange(scryptKey, 0, 32);
        byte[] derivedhalf2 = Arrays.copyOfRange(scryptKey, 32, 64);

        Log.d(TAG, "derivedhalf1=" + Arrays.toString(derivedhalf1) + " ,length=" + derivedhalf1.length);
        Log.d(TAG, "derivedhalf2=" + Arrays.toString(derivedhalf2) + " ,length=" + derivedhalf2.length);

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

    private byte[] doAES256Decrypt(byte[] input, byte[] key){
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


}