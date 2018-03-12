package gtoken.com.nep2.object;

/**
 * Created by furyvn on 3/12/18.
 */

public class Account {

    private String address;
    private String label;
    private boolean isDefault;
    private boolean lock;
    private String key;
    private Contract contract;
    private String extra;

    public Account(String address, String label, boolean isDefault, boolean lock, String key, Contract contract, String extra) {
        this.address = address;
        this.label = label;
        this.isDefault = isDefault;
        this.lock = lock;
        this.key = key;
        this.contract = contract;
        this.extra = extra;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
