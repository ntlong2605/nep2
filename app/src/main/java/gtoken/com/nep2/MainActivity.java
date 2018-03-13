package gtoken.com.nep2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import gtoken.com.nep2.object.Account;
import gtoken.com.nep2.object.Contract;
import gtoken.com.nep2.object.ScryptParam;
import gtoken.com.nep2.object.Wallet;
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
    private static final String mWIF = "KwYgW8gcxj1JWJXhPSu4Fqwzfhp5Yfi42mdYmMa4XqK7NJxXUSK7";

    private static final int N = 16384;
    private static final int r = 8;
    private static final int p = 8;

    private EditText mUserPassPhrase;
    private Button mCreateWallet, mRestoreWallet;
    private TextView mCreatedJsonView, mRestoreJsonView, mCreatedPrivateKey, mRestorePrivateKey;

    private static final int PICK_FILES_REQUEST_CODE = 1111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUserPassPhrase = findViewById(R.id.user_passphrase);
        mCreateWallet = findViewById(R.id.btn_create_wallet);
        mRestoreWallet = findViewById(R.id.btn_restore_wallet);
        mCreatedJsonView = findViewById(R.id.output_json);
        mRestoreJsonView = findViewById(R.id.output_restore_json);
        mCreatedPrivateKey = findViewById(R.id.create_new_private_key);
        mRestorePrivateKey = findViewById(R.id.restore_private_key);

        mCreateWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String userPassPhrase = mUserPassPhrase.getText().toString();
                if (!TextUtils.isEmpty(userPassPhrase)) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    } else {
                        createWallet(userPassPhrase);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Passphrase cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mRestoreWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0:
                createWallet(mUserPassPhrase.getText().toString());
                break;
        }
    }

    private void openFile() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, PICK_FILES_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PICK_FILES_REQUEST_CODE:

                    String filePath = "", fileName = "";

                    if (data != null && data.getData() != null) {
                        filePath = data.getData().getPath();
                        fileName = data.getData().getLastPathSegment();
                        Log.d(TAG, "path=" + filePath);
                        Log.d(TAG, "name=" + fileName);
                    }

                    if (!TextUtils.isEmpty(filePath)) {
                        try {
                            File jsonWallet = new File(filePath);
                            FileInputStream stream = new FileInputStream(jsonWallet);
                            String jsonStr;
                            try {
                                FileChannel fc = stream.getChannel();
                                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                                jsonStr = Charset.defaultCharset().decode(bb).toString();
                                mRestoreJsonView.setText(jsonStr);
                                Gson gson = new Gson();
                                Wallet wallet = gson.fromJson(jsonStr, Wallet.class);

                                //TODO: get the fist item just for testing
                                String encryptedNEP2 = wallet.getAccounts().get(0).getKey();
                                Log.d(TAG, "encryptedNEP2=" + encryptedNEP2);
                                String userPassPhrase = mUserPassPhrase.getText().toString();
                                Log.d(TAG, "userPassPhrase=" + userPassPhrase);

                                if (!TextUtils.isEmpty(userPassPhrase) && !TextUtils.isEmpty(encryptedNEP2)) {
                                    mRestorePrivateKey.setText(decrypt(userPassPhrase, encryptedNEP2));
                                } else {
                                    Toast.makeText(MainActivity.this, "userPassPhrase or encryptedNEP2 was empty", Toast.LENGTH_SHORT).show();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(MainActivity.this, "Cannot read file", Toast.LENGTH_SHORT).show();
                            } finally {
                                stream.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Cannot read file", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(MainActivity.this, "Cannot read file", Toast.LENGTH_SHORT).show();
                    }

                    break;
            }
        }
    }

    private void writeJsonToFile(String json) {
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
            Toast.makeText(MainActivity.this, "wallet.json created at " + jsonWallet.getPath(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "wallet.json created at " + jsonWallet.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createWallet(String userPassPhrase) {
        String privateKey = genPrivateKeyRandomly();
        Log.d(TAG, "random private key=" + privateKey);
        String address = genPublicKeyAndAddress(privateKeyToWIF(privateKey));
        String encryptedNEP2 = encrypt(address, userPassPhrase, privateKey);

        ScryptParam scryptParam = new ScryptParam(16384, 8, 8);
        List<Account> accountList = new ArrayList<>();
        Account account = new Account(address, "null", false, false, encryptedNEP2, new Contract(), "null");
        accountList.add(account);

        Wallet wallet = new Wallet();
        wallet.setName("MyWallet");
        wallet.setVersion("1.0");
        wallet.setScrypt(scryptParam);
        wallet.setAccounts(accountList);
        wallet.setExtra("null");

        Gson gson = new Gson();
        String json = gson.toJson(wallet);
        mCreatedJsonView.setText(json);
        mCreatedPrivateKey.setText(privateKey);
        writeJsonToFile(json);
    }

    private String genPrivateKeyRandomly() {
        SecureRandom random = new SecureRandom();
        byte[] privateKey = new byte[32];
        random.nextBytes(privateKey);
        return bytesToHex(privateKey);
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }


    private String encrypt(String address, String passPhrase, String rawPrivateKey) {
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
        byte[] encryptedKey = doAES256Encrypt(xor, derivedhalf2);
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

    private String decrypt(String passPhrase, String encryptedNEP2) {

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
        byte[] xor = doAES256Decrypt(encryptedPrivateKey, derivedhalf2);

        //form private key
        byte[] privateKeyByteArray = doXor(xor, derivedhalf1);
        String rawPrivateKey = bytesToHex(privateKeyByteArray);
        Log.d(TAG, "raw private key=" + rawPrivateKey);

        return rawPrivateKey;

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
    private String privateKeyToWIF(String rawPrivateKey) {

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

    /**
     * https://brainwalletx.github.io/#generator --> test tool
     */
    private String genPublicKeyAndAddress(String wif) {
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


}
