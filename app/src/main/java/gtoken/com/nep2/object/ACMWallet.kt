package gtoken.com.nep2.`object`

import neoutils.Wallet

import neoutils.Neoutils.generatePublicKeyFromPrivateKey

/**
 * Created by furyvn on 3/12/18.
 */

class ACMWallet {

    var name: String? = null
    var version: String? = null
    var scrypt: ScryptParam? = null
    var accounts: List<Account>? = null
    var extra: String? = null

}
