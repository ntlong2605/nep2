package com.gcox.wallet.`object`

import neoutils.Wallet

import neoutils.Neoutils.generatePublicKeyFromPrivateKey

/**
 * Created by furyvn on 3/13/18.
 */

class O3Wallet(rawPrivateKey: String) {

    private var mO3Wallet: Wallet? = null
    lateinit var wif: String
    lateinit var address: String

    init {
        try {
            mO3Wallet = generatePublicKeyFromPrivateKey(rawPrivateKey)
            this.wif = mO3Wallet!!.wif
            this.address = mO3Wallet!!.address
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}
