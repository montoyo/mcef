package net.montoyo.mcef.api;

import net.minecraftforge.fml.ModList;

public class MCEFApi {
    
    /**
     * Call this to get the API instance.
     * @return the MCEF API or null if something failed.
     */
    public static API getAPI() {
        try {
            Class cls = Class.forName("net.montoyo.mcef.MCEF");
            return (API) cls.getField("PROXY").get(null);
        } catch(Throwable t) {
            System.out.println("API Instance retrieval failure!");
            t.printStackTrace();
            return null;
        }
    }
    
    /**
     * Checks if MCEF was loaded by forge.
     * @return true if it is loaded. false otherwise.
     */
    public static boolean isMCEFLoaded() {
        return  ModList.get().isLoaded("forgecef");
    }

}
