package net.montoyo.mcef;

import net.montoyo.mcef.utilities.Log;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(modid = "MCEF", name = "MCEF", version = MCEF.VERSION)
public class MCEF {
	
	public static final String VERSION = "0.4";
	
	@Mod.Instance
	public static MCEF INSTANCE;
	
	@SidedProxy(serverSide = "net.montoyo.mcef.BaseProxy", clientSide = "net.montoyo.mcef.client.ClientProxy")
	public static BaseProxy PROXY;
	
	@Mod.EventHandler
	public void onInit(FMLInitializationEvent ev) {
		Log.info("Now initializing MCEF v%s...", VERSION);
		PROXY.onInit();
	}

}
