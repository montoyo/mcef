package com.cinemamod.mcef;

import com.cinemamod.mcef.example.MCEFExampleMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class FabricMCEFClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            new MCEFExampleMod();
        }
    }
}
