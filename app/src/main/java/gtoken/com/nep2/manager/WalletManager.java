package gtoken.com.nep2.manager;

/**
 * Created by furyvn on 3/13/18.
 */

public class WalletManager {

    private WalletManager() {
    }

    private static WalletManager INSTANCE;

    public static WalletManager getInstance() {

        if (INSTANCE == null) {
            return new WalletManager();
        }

        return INSTANCE;
    }


}
