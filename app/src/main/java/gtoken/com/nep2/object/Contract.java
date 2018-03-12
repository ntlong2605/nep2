package gtoken.com.nep2.object;

/**
 * Created by furyvn on 3/12/18.
 */

public class Contract {

    private String script;
    private String parameters; //TODO: this property is a list
    private boolean deployed;

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public boolean isDeployed() {
        return deployed;
    }

    public void setDeployed(boolean deployed) {
        this.deployed = deployed;
    }


}
