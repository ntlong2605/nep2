package gtoken.com.nep2.manager;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import gtoken.com.nep2.object.ACMWallet;
import gtoken.com.nep2.object.Account;
import gtoken.com.nep2.object.Contract;
import gtoken.com.nep2.object.O3Wallet;
import gtoken.com.nep2.object.ScryptParam;
import gtoken.com.nep2.util.WalletUtils;

/**
 * Created by furyvn on 3/13/18.
 */

public class WalletManager {

    private static final String TAG = WalletManager.class.getSimpleName();

    private WalletManager() {
    }

    private static WalletManager INSTANCE = null;

    public static WalletManager getInstance() {

        if (INSTANCE == null) {
            return new WalletManager();
        }

        return INSTANCE;
    }

    public enum WalletType {
        ACM_WALLET,
        O3_WALLET,
        UNKNOWN
    }

    private ACMWallet mACMWallet;

    public void createWallet(String userPassPhrase, WalletType walletType) {

        switch (walletType) {
            case ACM_WALLET:
                genACMWallet(userPassPhrase);
                break;
        }
    }

    private void genACMWallet(String userPassPhrase) {
        String rawPrivate = WalletUtils.genPrivateKeyRandomly();
        ScryptParam scryptParam = new ScryptParam(16384, 8, 8);
        List<Account> accountList = new ArrayList<>();

        this.mACMWallet = new ACMWallet();
        O3Wallet o3Wallet = new O3Wallet(rawPrivate);
        mACMWallet.setName("MyWallet");
        mACMWallet.setVersion("1.0");
        mACMWallet.setScrypt(scryptParam);
        mACMWallet.setExtra("null");

        String address = o3Wallet.getAddress();
        String encryptedNEP2 = WalletUtils.encryptNEP2(address, userPassPhrase, rawPrivate);

        //Create default account, mark isDefault = true
        Account account = new Account(address, "null", true, false, encryptedNEP2, new Contract(), "null");
        account.setPrivateKey(rawPrivate);
        account.setWIF(o3Wallet.getWIF());
        account.setAddress(address);
        accountList.add(account);

        mACMWallet.setAccounts(accountList);
        Log.d(TAG, ".Inside genACMWallet, privKey=" + account.getPrivateKey() + " ,WIF=" + account.getWIF() + " ,address=" + account.getAddress());

        //JUST TESTING
        exportWallet(WalletType.ACM_WALLET);
    }

    public void restoreWallet(String userPassPhrase, String importedJSON, WalletType walletType) {
        switch (walletType) {
            case ACM_WALLET:

                this.mACMWallet = new ACMWallet();
                List<Account> accountList = new ArrayList<>();

                try {
                    JSONObject walletObject = new JSONObject(importedJSON);
                    mACMWallet.setName(walletObject.getString("name"));
                    mACMWallet.setVersion(walletObject.getString("version"));
                    ScryptParam scryptParam = new ScryptParam(16384, 8, 8);
                    mACMWallet.setScrypt(scryptParam);

                    JSONArray acountList = walletObject.getJSONArray("accounts");
                    for (int i = 0; i < acountList.length(); i++) {
                        JSONObject accountObject = acountList.getJSONObject(i);

                        Account account = new Account(accountObject.getString("address"),
                                accountObject.getString("label"),
                                accountObject.getBoolean("isDefault"),
                                accountObject.getBoolean("lock"),
                                accountObject.getString("key"),
                                new Contract(),
                                accountObject.getString("extra"));

                        accountList.add(account);

                        String rawPrivate = WalletUtils.decryptNEP2(userPassPhrase, accountObject.getString("key"));
                        account.setPrivateKey(rawPrivate);
                        O3Wallet o3Wallet = new O3Wallet(rawPrivate);
                        account.setWIF(o3Wallet.getWIF());
                        Log.d(TAG, ".Inside restoreWallet, privKey=" + rawPrivate + " ,WIF=" + account.getWIF() + " ,address=" + account.getAddress());
                    }
                    mACMWallet.setAccounts(accountList);
                    mACMWallet.setExtra(walletObject.getString("extra"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }

    }

    private void exportWallet(WalletType walletType) {
        switch (walletType) {
            case ACM_WALLET:
                Gson gson = new Gson();
                String json = gson.toJson(mACMWallet);
                WalletUtils.writeJsonToFile(json);
                break;
        }

    }


}
