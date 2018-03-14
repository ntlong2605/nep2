package gtoken.com.nep2.util

import android.os.Environment
import android.util.Log
import gtoken.com.nep2.scrypt.crypto.SCryptUtil
import org.bitcoinj.core.ECKey
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.*
import kotlin.experimental.xor

/*
 * Created by furyvn on 3/13/18.
 */

class WalletUtils {

    private fun wifToPrivateKey(wif: String) {
        val wifByteArray = Base58.decode(wif)

        //remove the first, the last byte and checksum
        val privateKey = Arrays.copyOfRange(wifByteArray, 1, 33)

        Log.d(TAG, "private key=" + bytesToHex(privateKey))

    }

    companion object {

        private val TAG = WalletUtils::class.java.simpleName
        private val N = 16384
        private val r = 8
        private val p = 8

        fun encryptNEP2(address: String, passPhrase: String, rawPrivateKey: String): String {
            /*
         * Step 1: Compute the NEO address (ASCII), and take the first four bytes of SHA256(SHA256()) of it. Let's call this "addresshash".
         */
            val doubleSHA256Hash = SHA256HashUtil.getDoubleSHA256Hash(address.toByteArray())
            Log.d(TAG, "doubleSHA256Hash=" + Arrays.toString(doubleSHA256Hash))

            //get the first 4 bytes
            val salt = ByteArray(4)
            System.arraycopy(doubleSHA256Hash, 0, salt, 0, 4)
            Log.d(TAG, "salt=" + Arrays.toString(salt))

            /*
         * Step 2: Derive a key from the passphrase using Scrypt
         */
            val scryptKey = SCryptUtil.scrypt(passPhrase, salt, N, r, p)
            Log.d(TAG, "scryptKey=" + Arrays.toString(scryptKey) + " ,length=" + scryptKey.size)

            //Split derive key into 2 parts
            val derivedhalf1 = Arrays.copyOfRange(scryptKey, 0, 32)
            val derivedhalf2 = Arrays.copyOfRange(scryptKey, 32, 64)
            Log.d(TAG, "derivedhalf1=" + Arrays.toString(derivedhalf1) + " ,length=" + derivedhalf1.size)
            Log.d(TAG, "derivedhalf2=" + Arrays.toString(derivedhalf2) + " ,length=" + derivedhalf2.size)

            /*
         * Step 3: out encryptedkey
         */
            val privateKeyByteArray = hexStringToByteArray(rawPrivateKey)
            Log.d(TAG, "privateKeyByteArray=" + Arrays.toString(privateKeyByteArray) + " ,length=" + privateKeyByteArray.size)

            //Do AES256 encrypt
            val xor = doXor(privateKeyByteArray, derivedhalf1)
            val encryptedKey = AES256Utils.doAES256Encrypt(xor, derivedhalf2)
            Log.d(TAG, "encryptedkey=" + Arrays.toString(encryptedKey) + " ,length=" + encryptedKey!!.size)

            //Combine into one: 0x01 0x42 + flagbyte + salt + encryptedkey
            val prefix = hexStringToByteArray("0142")
            val flagbyte = hexStringToByteArray("E0")
            val bb = ByteBuffer.allocate(prefix.size + flagbyte.size + salt.size + encryptedKey.size)
            bb.put(prefix)
            bb.put(flagbyte)
            bb.put(salt)
            bb.put(encryptedKey)
            val payload = bb.array()

            //Add checksum
            val payloadDoubleHash = SHA256HashUtil.getDoubleSHA256Hash(payload)
            val checkSum = ByteArray(4)
            System.arraycopy(payloadDoubleHash, 0, checkSum, 0, checkSum.size)
            val bb2 = ByteBuffer.allocate(payload.size + checkSum.size)
            bb2.put(payload)
            bb2.put(checkSum)
            val encryptedPrivateKey = bb2.array()

            Log.d(TAG, "encryptedPrivateKey=" + Arrays.toString(encryptedPrivateKey) + " ,length=" + encryptedPrivateKey.size)
            val base58 = Base58.encode(encryptedPrivateKey)
            Log.d(TAG, "Base58 encryptedPrivateKey=" + base58 + " ,length=" + base58.length)

            return base58
        }

        fun decryptNEP2(passPhrase: String, encryptedNEP2: String): String {

            //decrypt payload
            val decryptedPayload = Base58.decode(encryptedNEP2)

            //decrypt salt
            val salt = Arrays.copyOfRange(decryptedPayload, 3, 7)
            Log.d(TAG, "salt=" + Arrays.toString(salt) + " ,length=" + salt.size)

            //decrypt encrypted private key
            val encryptedPrivateKey = Arrays.copyOfRange(decryptedPayload, 7, 39)
            Log.d(TAG, "encryptedPrivateKey=" + Arrays.toString(encryptedPrivateKey) + " ,length=" + encryptedPrivateKey.size)

            //Devive Scrypt key
            val scryptKey = SCryptUtil.scrypt(passPhrase, salt, N, r, p)
            Log.d(TAG, "scryptKey=" + Arrays.toString(scryptKey) + "length=" + scryptKey.size)

            //Split derive key into 2 parts
            val derivedhalf1 = Arrays.copyOfRange(scryptKey, 0, 32)
            val derivedhalf2 = Arrays.copyOfRange(scryptKey, 32, 64)
            Log.d(TAG, "derivedhalf1=" + Arrays.toString(derivedhalf1) + " ,length=" + derivedhalf1.size)
            Log.d(TAG, "derivedhalf2=" + Arrays.toString(derivedhalf2) + " ,length=" + derivedhalf2.size)

            //DecryptSHA256 to get XOR
            val xor = AES256Utils.doAES256Decrypt(encryptedPrivateKey, derivedhalf2)

            //form private key
            val privateKeyByteArray = doXor(xor, derivedhalf1)
            val rawPrivateKey = bytesToHex(privateKeyByteArray)
            Log.d(TAG, "raw private key=$rawPrivateKey")

            return rawPrivateKey

        }

        private fun hexStringToByteArray(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }

        private fun doXor(data1: ByteArray?, data2: ByteArray): ByteArray {
            var data1 = data1
            var data2 = data2
            // make data2 the largest...
            if (data1!!.size > data2.size) {
                val tmp = data2
                data2 = data1
                data1 = tmp
            }
            for (i in data1.indices) {
                data2[i] = data2[i] xor data1[i]
            }
            return data2
        }

        private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

        private fun bytesToHex(bytes: ByteArray): String {
            val result = StringBuffer()

            for (i in bytes) {
                val octet = i.toInt()
                val firstIndex = (octet and 0xF0).ushr(4)
                val secondIndex = octet and 0x0F
                result.append(HEX_CHARS[firstIndex])
                result.append(HEX_CHARS[secondIndex])
            }

            return result.toString()
        }

        fun genPrivateKeyRandomly(): String {
            val random = SecureRandom()
            val privateKey = ByteArray(32)
            random.nextBytes(privateKey)
            return bytesToHex(privateKey)
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
        fun privateKeyToWIF(rawPrivateKey: String): String {

            val privateKey = hexStringToByteArray(rawPrivateKey)
            Log.d(TAG, "privateKey=" + Arrays.toString(privateKey) + " ,length=" + privateKey.size)

            //Add 0x80 byte to the front, add 0x01 to the end
            val versionByte = hexStringToByteArray("80")
            val lastByte = hexStringToByteArray("01")
            val bb = ByteBuffer.allocate(privateKey.size + versionByte.size + lastByte.size)
            bb.put(versionByte)
            bb.put(privateKey)
            bb.put(lastByte)
            val versionByteAndPrivateKey = bb.array()
            Log.d(TAG, "versionByteAndPrivateKey=" + Arrays.toString(versionByteAndPrivateKey) + " ,length=" + versionByteAndPrivateKey.size)

            //double hash
            val double256Hash = SHA256HashUtil.getDoubleSHA256Hash(versionByteAndPrivateKey)

            //get checksum
            val checkSum = Arrays.copyOfRange(double256Hash, 0, 4)
            Log.d(TAG, "checkSum=" + Arrays.toString(checkSum) + " ,length=" + checkSum.size)

            //Adding checksum at the end of addedByteAndPrivateKey
            val bb2 = ByteBuffer.allocate(versionByteAndPrivateKey.size + checkSum.size)
            bb2.put(versionByteAndPrivateKey)
            bb2.put(checkSum)
            val wif = bb2.array()

            Log.d(TAG, "wif=" + Arrays.toString(wif) + " ,length=" + wif.size)

            //Encode Base58
            Log.d(TAG, "WIF=" + Base58.encode(wif))

            return Base58.encode(wif)

        }

        fun genPublicKeyAndAddress(wif: String): String {
            // An example of private key from the book 'Mastering Bitcoin'
            //wif = "a71b25fd2fcf01eb3a4496405ce53f896ec075d1c9386f26e52805676b1953e2";

            val key = hexStringToByteArray(wif)

            // Creating a key object from our private key, with compressed public key
            val k1 = ECKey.fromPrivate(key, true)

            // Creating a key object from our private key, with uncompressed public key
            val k2 = ECKey.fromPrivate(key, false)


            Log.d(TAG, "compressed public address=" + k1.publicKeyAsHex) // compressed
            Log.d(TAG, "uncompressed public address=" + k2.publicKeyAsHex) // uncompressed

            val main = MainNetParams.get()   // main bitcoin network
            val test = TestNet3Params.get()  // genPublicKeyAndAddress bitcoin network

            val addr1 = k1.toAddress(main) // main/live network, compressed
            val addr2 = k1.toAddress(test) // test network, compressed
            val addr3 = k2.toAddress(main) // main/live network, uncompressed
            val addr4 = k2.toAddress(test) // test network, uncompressed

            Log.d(TAG, "main network, compressed=" + addr1.toString())
            Log.d(TAG, "test network, compressed=" + addr2.toString())
            Log.d(TAG, "main network, uncompressed=" + addr3.toString())
            Log.d(TAG, "test network, uncompressed=" + addr4.toString())

            return addr1.toString()
        }

        fun writeJsonToFile(json: String) {
            val filename = "wallet.json"
            val filepath = "/NEOCrypto"

            val sdCard = Environment.getExternalStorageDirectory()
            val dir = File(sdCard.absolutePath + filepath)
            dir.mkdirs()
            val jsonWallet = File(dir, filename)

            try {
                val fos = FileOutputStream(jsonWallet)
                fos.write(json.toByteArray())
                fos.close()
                Log.d(TAG, "wallet.json created at " + jsonWallet.path)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

}
