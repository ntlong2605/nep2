package com.gcox.wallet.manager

import android.util.Log
import com.google.gson.Gson
import com.gcox.wallet.`object`.*
import com.gcox.wallet.util.WalletUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Created by furyvn on 3/13/18.
 */

class WalletManager private constructor() {

    private var mACMWallet: ACMWallet? = null

    enum class WalletType {
        ACM_WALLET,
        O3_WALLET,
        UNKNOWN
    }

    fun createWallet(userPassPhrase: String, walletType: WalletType) {

        when (walletType) {
            WalletManager.WalletType.ACM_WALLET -> genACMWallet(userPassPhrase)
        }
    }

    private fun genACMWallet(userPassPhrase: String) {
        val rawPrivate = WalletUtils.genPrivateKeyRandomly()
        val scryptParam = ScryptParam(16384, 8, 8)
        val accountList = ArrayList<Account>()

        this.mACMWallet = ACMWallet()
        val o3Wallet = O3Wallet(rawPrivate)
        mACMWallet!!.name = "MyWallet"
        mACMWallet!!.version = "1.0"
        mACMWallet!!.scrypt = scryptParam
        mACMWallet!!.extra = "null"

        val address = o3Wallet.address
        val encryptedNEP2 = WalletUtils.encryptNEP2(address, userPassPhrase, rawPrivate)

        //Create default account, mark isDefault = true
        val account = Account(address, "null", true, false, encryptedNEP2, Contract(), "null")
        account.privateKey = rawPrivate
        account.wif = o3Wallet.wif
        account.address = address
        accountList.add(account)

        mACMWallet!!.accounts = accountList
        Log.d(TAG, ".Inside genACMWallet, privKey=" + account.privateKey + " ,WIF=" + account.wif + " ,address=" + account.address)

        //JUST TESTING
        exportWallet(WalletType.ACM_WALLET)
    }

    fun restoreWallet(userPassPhrase: String, importedJSON: String, walletType: WalletType) {
        when (walletType) {
            WalletManager.WalletType.ACM_WALLET -> {

                this.mACMWallet = ACMWallet()
                val accountList = ArrayList<Account>()

                try {
                    val walletObject = JSONObject(importedJSON)
                    mACMWallet!!.name = walletObject.getString("name")
                    mACMWallet!!.version = walletObject.getString("version")
                    val scryptParam = ScryptParam(16384, 8, 8)
                    mACMWallet!!.scrypt = scryptParam

                    val acountList = walletObject.getJSONArray("accounts")
                    for (i in 0 until acountList.length()) {
                        val accountObject = acountList.getJSONObject(i)

                        val account = Account(accountObject.getString("address"),
                                accountObject.getString("label"),
                                accountObject.getBoolean("isDefault"),
                                accountObject.getBoolean("isLock"),
                                accountObject.getString("key"),
                                Contract(),
                                accountObject.getString("extra"))

                        accountList.add(account)

                        val rawPrivate = WalletUtils.decryptNEP2(userPassPhrase, accountObject.getString("key"))
                        account.privateKey = rawPrivate
                        val o3Wallet = O3Wallet(rawPrivate)
                        account.wif = o3Wallet.wif
                        Log.d(TAG, ".Inside restoreWallet, privKey=" + rawPrivate + " ,WIF=" + account.wif + " ,address=" + account.address)
                    }
                    mACMWallet!!.accounts = accountList
                    mACMWallet!!.extra = walletObject.getString("extra")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }

            else -> {
                //TODO
            }
        }

    }

    private fun exportWallet(walletType: WalletType) {
        when (walletType) {
            WalletType.ACM_WALLET -> {
                val gson = Gson()
                val json = gson.toJson(mACMWallet)
                WalletUtils.writeJsonToFile(json)
            }

            else -> {
                //TODO
            }
        }

    }

    companion object {

        private val TAG = WalletManager::class.java.simpleName

        private val INSTANCE: WalletManager? = null

        val instance: WalletManager
            get() = INSTANCE ?: WalletManager()
    }


}
