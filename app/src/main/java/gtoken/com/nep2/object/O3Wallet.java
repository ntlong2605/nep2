package gtoken.com.nep2.object;

import neoutils.Wallet;

import static neoutils.Neoutils.generatePublicKeyFromPrivateKey;

/**
 * Created by furyvn on 3/13/18.
 */

public class O3Wallet {

    private Wallet mO3Wallet;
    private String WIF;
    private String address;

    public String getWIF() {
        return WIF;
    }

    public void setWIF(String WIF) {
        this.WIF = WIF;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public O3Wallet(String rawPrivateKey) {
        try {
            mO3Wallet = generatePublicKeyFromPrivateKey(rawPrivateKey);
            this.WIF = mO3Wallet.getWIF();
            this.address = mO3Wallet.getAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
