package com.cinemamod.mcef;

import com.cinemamod.mcef.example.MCEFExampleMod;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(ForgeMCEFMod.MODID)
public class ForgeMCEFMod {
    public static final String MODID = "mcef";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ForgeMCEFMod() {
        MinecraftForge.EVENT_BUS.register(this);

        if (!FMLEnvironment.production) {
            new MCEFExampleMod();
        }
    }
}
