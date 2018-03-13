package gtoken.com.nep2.util;

import android.os.Environment;
import android.util.Log;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import gtoken.com.nep2.scrypt.crypto.SCryptUtil;

/**
 * Created by furyvn on 3/13/18.
 */

public class WalletUtils {

    private static final String TAG = WalletUtils.class.getSimpleName();
    private static final int N = 16384;
    private static final int r = 8;
    private static final int p = 8;

    public static String encryptNEP2(String address, String passPhrase, String rawPrivateKey) {
        /*
         * Step 1: Compute the NEO address (ASCII), and take the first four bytes of SHA256(SHA256()) of it. Let's call this "addresshash".
         */
        byte[] doubleSHA256Hash = SHA256HashUtil.getDoubleSHA256Hash(address.getBytes());
        Log.d(TAG, "doubleSHA256Hash=" + Arrays.toString(doubleSHA256Hash));

        //get the first 4 bytes
        byte[] salt = new byte[4];
        System.arraycopy(doubleSHA256Hash, 0, salt, 0, 4);
        Log.d(TAG, "salt=" + Arrays.toString(salt));

        /*
         * Step 2: Derive a key from the passphrase using Scrypt
         */
        byte[] scryptKey = SCryptUtil.scrypt(passPhrase, salt, N, r, p);
        Log.d(TAG, "scryptKey=" + Arrays.toString(scryptKey) + " ,length=" + scryptKey.length);

        //Split derive key into 2 parts
        byte[] derivedhalf1 = Arrays.copyOfRange(scryptKey, 0, 32);
        byte[] derivedhalf2 = Arrays.copyOfRange(scryptKey, 32, 64);
        Log.d(TAG, "derivedhalf1=" + Arrays.toString(derivedhalf1) + " ,length=" + derivedhalf1.length);
        Log.d(TAG, "derivedhalf2=" + Arrays.toString(derivedhalf2) + " ,length=" + derivedhalf2.length);

        /*
         * Step 3: out encryptedkey
         */
        byte[] privateKeyByteArray = hexStringToByteArray(rawPrivateKey);
        Log.d(TAG, "privateKeyByteArray=" + Arrays.toString(privateKeyByteArray) + " ,length=" + privateKeyByteArray.length);

        //Do AES256 encrypt
        byte[] xor = doXor(privateKeyByteArray, derivedhalf1);
        byte[] encryptedKey = AES256Utils.doAES256Encrypt(xor, derivedhalf2);
        Log.d(TAG, "encryptedkey=" + Arrays.toString(encryptedKey) + " ,length=" + encryptedKey.length);

        //Combine into one: 0x01 0x42 + flagbyte + salt + encryptedkey
        byte[] prefix = hexStringToByteArray("0142");
        byte[] flagbyte = hexStringToByteArray("E0");
        ByteBuffer bb = ByteBuffer.allocate(prefix.length + flagbyte.length + salt.length + encryptedKey.length);
        bb.put(prefix);
        bb.put(flagbyte);
        bb.put(salt);
        bb.put(encryptedKey);
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

        return base58;
    }

    public static String decryptNEP2(String passPhrase, String encryptedNEP2) {

        //decrypt payload
        byte[] decryptedPayload = Base58.decode(encryptedNEP2);

        //decrypt salt
        byte[] salt = Arrays.copyOfRange(decryptedPayload, 3, 7);
        Log.d(TAG, "salt=" + Arrays.toString(salt) + " ,length=" + salt.length);

        //decrypt encrypted private key
        byte[] encryptedPrivateKey = Arrays.copyOfRange(decryptedPayload, 7, 39);
        Log.d(TAG, "encryptedPrivateKey=" + Arrays.toString(encryptedPrivateKey) + " ,length=" + encryptedPrivateKey.length);

        //Devive Scrypt key
        byte[] scryptKey = SCryptUtil.scrypt(passPhrase, salt, N, r, p);
        Log.d(TAG, "scryptKey=" + Arrays.toString(scryptKey) + "length=" + scryptKey.length);

        //Split derive key into 2 parts
        byte[] derivedhalf1 = Arrays.copyOfRange(scryptKey, 0, 32);
        byte[] derivedhalf2 = Arrays.copyOfRange(scryptKey, 32, 64);
        Log.d(TAG, "derivedhalf1=" + Arrays.toString(derivedhalf1) + " ,length=" + derivedhalf1.length);
        Log.d(TAG, "derivedhalf2=" + Arrays.toString(derivedhalf2) + " ,length=" + derivedhalf2.length);

        //DecryptSHA256 to get XOR
        byte[] xor = AES256Utils.doAES256Decrypt(encryptedPrivateKey, derivedhalf2);

        //form private key
        byte[] privateKeyByteArray = doXor(xor, derivedhalf1);
        String rawPrivateKey = bytesToHex(privateKeyByteArray);
        Log.d(TAG, "raw private key=" + rawPrivateKey);

        return rawPrivateKey;

    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static byte[] doXor(byte[] data1, byte[] data2) {
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

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String genPrivateKeyRandomly() {
        SecureRandom random = new SecureRandom();
        byte[] privateKey = new byte[32];
        random.nextBytes(privateKey);
        return bytesToHex(privateKey);
    }

    /*
     * ref link: https://bitcointalk.org/index.php?topic=129652.0
     * If you need to accept WIF as input, after the base58decode step, you'll always have 33 or 34 bytes.
     * If 33 bytes, it is uncompressed, and the private key is the 32 bytes after the 0x80 header.
     * If 34 bytes, it is compressed, the last byte must be 0x01, and the private key is everything between the 0x80 header and the 0x01 compression flag.
     * Once you have the private key and know whether the compression flag was present or not, you can calculate the public key and address as above.
     */

    /**
     * 0x01 compression flag, if we add lastByte to the end, function will out compressed WTF, otherwise uncompressed
     */
    public static String privateKeyToWIF(String rawPrivateKey) {

        byte[] privateKey = hexStringToByteArray(rawPrivateKey);
        Log.d(TAG, "privateKey=" + Arrays.toString(privateKey) + " ,length=" + privateKey.length);

        //Add 0x80 byte to the front, add 0x01 to the end
        byte[] versionByte = hexStringToByteArray("80");
        byte[] lastByte = hexStringToByteArray("01");
        ByteBuffer bb = ByteBuffer.allocate(privateKey.length + versionByte.length + lastByte.length);
        bb.put(versionByte);
        bb.put(privateKey);
        bb.put(lastByte);
        byte[] versionByteAndPrivateKey = bb.array();
        Log.d(TAG, "versionByteAndPrivateKey=" + Arrays.toString(versionByteAndPrivateKey) + " ,length=" + versionByteAndPrivateKey.length);

        //double hash
        byte[] double256Hash = SHA256HashUtil.getDoubleSHA256Hash(versionByteAndPrivateKey);

        //get checksum
        byte[] checkSum = Arrays.copyOfRange(double256Hash, 0, 4);
        Log.d(TAG, "checkSum=" + Arrays.toString(checkSum) + " ,length=" + checkSum.length);

        //Adding checksum at the end of addedByteAndPrivateKey
        ByteBuffer bb2 = ByteBuffer.allocate(versionByteAndPrivateKey.length + checkSum.length);
        bb2.put(versionByteAndPrivateKey);
        bb2.put(checkSum);
        byte[] wif = bb2.array();

        Log.d(TAG, "wif=" + Arrays.toString(wif) + " ,length=" + wif.length);

        //Encode Base58
        Log.d(TAG, "WIF=" + Base58.encode(wif));

        return Base58.encode(wif);

    }

    private void wifToPrivateKey(String wif) {
        byte[] wifByteArray = Base58.decode(wif);

        //remove the first, the last byte and checksum
        byte[] privateKey = Arrays.copyOfRange(wifByteArray, 1, 33);

        Log.d(TAG, "private key=" + bytesToHex(privateKey));

    }

    public static String genPublicKeyAndAddress(String wif) {
        // An example of private key from the book 'Mastering Bitcoin'
        //wif = "a71b25fd2fcf01eb3a4496405ce53f896ec075d1c9386f26e52805676b1953e2";

        byte[] key = hexStringToByteArray(wif);

        // Creating a key object from our private key, with compressed public key
        ECKey k1 = ECKey.fromPrivate(key, true);

        // Creating a key object from our private key, with uncompressed public key
        ECKey k2 = ECKey.fromPrivate(key, false);


        Log.d(TAG, "compressed public address=" + k1.getPublicKeyAsHex()); // compressed
        Log.d(TAG, "uncompressed public address=" + k2.getPublicKeyAsHex()); // uncompressed

        NetworkParameters main = MainNetParams.get();   // main bitcoin network
        NetworkParameters test = TestNet3Params.get();  // genPublicKeyAndAddress bitcoin network

        Address addr1 = k1.toAddress(main); // main/live network, compressed
        Address addr2 = k1.toAddress(test); // test network, compressed
        Address addr3 = k2.toAddress(main); // main/live network, uncompressed
        Address addr4 = k2.toAddress(test); // test network, uncompressed

        Log.d(TAG, "main network, compressed=" + addr1.toString());
        Log.d(TAG, "test network, compressed=" + addr2.toString());
        Log.d(TAG, "main network, uncompressed=" + addr3.toString());
        Log.d(TAG, "test network, uncompressed=" + addr4.toString());

        return addr1.toString();
    }

    public static void writeJsonToFile(String json) {
        String filename = "wallet.json";
        String filepath = "/NEOCrypto";

        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + filepath);
        dir.mkdirs();
        File jsonWallet = new File(dir, filename);

        try {
            FileOutputStream fos = new FileOutputStream(jsonWallet);
            fos.write(json.getBytes());
            fos.close();
            Log.d(TAG, "wallet.json created at " + jsonWallet.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}