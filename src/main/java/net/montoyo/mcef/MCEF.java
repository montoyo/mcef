package net.montoyo.mcef;

import net.montoyo.mcef.utilities.Log;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = "mcef", name = "MCEF", version = MCEF.VERSION)
public class MCEF {
    
    public static final String VERSION = "0.10";
    public static boolean ENABLE_EXAMPLE;
    public static boolean SKIP_UPDATES;
    public static boolean WARN_UPDATES;
    public static boolean USE_FORGE_SPLASH;
    public static String FORCE_MIRROR = null;
    public static String HOME_PAGE;
    public static boolean DISABLE_GPU_RENDERING;
    public static boolean CHECK_VRAM_LEAK;
    
    @Mod.Instance(owner = "mcef")
    public static MCEF INSTANCE;
    
    @SidedProxy(serverSide = "net.montoyo.mcef.BaseProxy", clientSide = "net.montoyo.mcef.client.ClientProxy")
    public static BaseProxy PROXY;
    
    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent ev) {
        Log.info("Loading MCEF config...");
        
        Configuration cfg = new Configuration(ev.getSuggestedConfigurationFile());
        SKIP_UPDATES = cfg.getBoolean("skipUpdates", "main", false, "Do not update binaries.");
        WARN_UPDATES = cfg.getBoolean("warnUpdates", "main", true, "Tells in the chat if a new version of MCEF is available.");
        USE_FORGE_SPLASH = cfg.getBoolean("useForgeSplash", "main", true, "Use Forge's splash screen to display resource download progress (may be unstable).");

        String mirror = cfg.getString("forcedMirror", "main", "", "A URL that contains every MCEF resources; for instance http://montoyo.net/jcef.").trim();
        if(mirror.length() > 0)
            FORCE_MIRROR = mirror;

        ENABLE_EXAMPLE = cfg.getBoolean("enable", "exampleBrowser", true, "Set this to false if you don't want to enable the F10 browser.");
        HOME_PAGE = cfg.getString("home", "exampleBrowser", "mod://mcef/home.html", "The home page of the F10 browser.");
        DISABLE_GPU_RENDERING = cfg.getBoolean("disableGPURendering", "main", true, "The new launcher breaks CEF GPU rendering. Re-enabling it may work with a different launcher like MultiMC.");
        CHECK_VRAM_LEAK = cfg.getBoolean("checkForVRAMLeak", "debug", false, "Track allocated OpenGL textures to make sure there's no leak");
        cfg.save();

        PROXY.onPreInit();
    }
    
    @Mod.EventHandler
    public void onInit(FMLInitializationEvent ev) {
        Log.info("Now initializing MCEF v%s...", VERSION);
        PROXY.onInit();
    }

    //Called by Minecraft.run() if the ShutdownPatcher succeeded
    public static void onMinecraftShutdown() {
        Log.info("Minecraft shutdown hook called!");
        PROXY.onShutdown();
    }

}
