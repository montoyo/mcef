package net.montoyo.mcef.easy_forge_compat;

public class Configuration {
    // TODO: actually use a fabric config system
    public boolean getBoolean(String optName, String section, boolean defaultValue, String desc) {
        return defaultValue;
    }

    public String getString(String optName, String section, String defaultValue, String desc) {
        return defaultValue;
    }

    public void save(){
        // TODO: Implement!
    }
}
