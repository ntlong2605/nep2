package gtoken.com.nep2.object;

/**
 * Created by furyvn on 3/12/18.
 */

public class ScryptParam {

    private int n;
    private int p;
    private int r;

    public ScryptParam(int n, int p, int r) {
        this.n = n;
        this.p = p;
        this.r = r;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public int getP() {
        return p;
    }

    public void setP(int p) {
        this.p = p;
    }

    public int getR() {
        return r;
    }

    public void setR(int r) {
        this.r = r;
    }

}
