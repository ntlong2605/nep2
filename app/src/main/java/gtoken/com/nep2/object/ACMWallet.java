package gtoken.com.nep2.object;

import java.util.List;

import neoutils.Wallet;

import static neoutils.Neoutils.generatePublicKeyFromPrivateKey;

/**
 * Created by furyvn on 3/12/18.
 */

public class ACMWallet {

    private String name;
    private String version;
    private ScryptParam scrypt;
    private List<Account> accounts;
    private String extra;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ScryptParam getScrypt() {
        return scrypt;
    }

    public void setScrypt(ScryptParam scrypt) {
        this.scrypt = scrypt;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

}
