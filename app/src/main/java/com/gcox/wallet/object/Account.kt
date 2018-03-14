package com.gcox.wallet.`object`

/**
 * Created by furyvn on 3/12/18.
 */

class Account(var address: String?, var label: String?, var isDefault: Boolean, var isLock: Boolean, var key: String?, var contract: Contract?, var extra: String?) {
    var privateKey: String? = null
    var wif: String? = null
    var publicKey: String? = null
}
