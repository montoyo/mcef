package net.montoyo.mcef.easy_forge_compat;

import com.google.gson.*;

import java.io.*;

public class Configuration {
    // Rewrote config to use json
    JsonObject root;
    static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Configuration(){
        File configDir = new File("config");
        if(!configDir.isDirectory()){
            configDir.mkdir();
        }
        File configFile = new File(configDir, "mcef.json");
        if(!configFile.isFile()){
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(configFile));
                bw.write("{\"useLegacyVersionWithCodecs\": false, \"skipLegacyVersionWarning\": false}");
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            root = JsonParser.parseReader(new BufferedReader(new FileReader(configFile))).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getBoolean(String optName, String section, boolean defaultValue, String desc) {
        if(root.has(optName)){
            JsonElement elem = root.get(optName);
            if(elem.isJsonPrimitive()){
                return elem.getAsBoolean();
            }
        }
        return defaultValue;
    }

    public String getString(String optName, String section, String defaultValue, String desc) {
        if(root.has(optName)){
            JsonElement elem = root.get(optName);
            if(elem.isJsonPrimitive()){
                return elem.getAsString();
            }
        }
        return defaultValue;
    }

    public void save(){
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("config/mcef.json"));
            bw.write(gson.toJson(root));
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
