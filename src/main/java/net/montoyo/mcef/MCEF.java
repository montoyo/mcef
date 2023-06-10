package net.montoyo.mcef;

import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.montoyo.mcef.client.ClientProxy;
import net.montoyo.mcef.easy_forge_compat.Configuration;
import net.montoyo.mcef.utilities.Log;

@Mod("forgecef")
public class MCEF {
	
	public static final String VERSION = "1.33";
	public static boolean ENABLE_EXAMPLE;
	public static String HOME_PAGE;
	public static String[] CEF_ARGS = new String[0];
	public static boolean CHECK_VRAM_LEAK;
	public static boolean SHUTDOWN_JCEF;
	
	// download options
	public static boolean SKIP_UPDATES;
	public static boolean WARN_UPDATES;
	public static String FORCE_MIRROR = null;
	public static boolean SECURE_MIRRORS_ONLY;
	public static boolean FAVOR_GIT;
	public static String[] FALLBACK_URLS_GIT;
	
	public static boolean HIGH_FPS;
	public static boolean ZERO_BUFFER;
	
	public static MCEF INSTANCE;
	
	public static BaseProxy PROXY = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> BaseProxy::new);
	public static boolean skipVersionCheck = false;
	public static boolean writeMirrorData = false;
	public static boolean downloadedFromGit = false;
	
	private static boolean wroteConfig = false;
	
	public static void ensureConfig() {
		if (wroteConfig) return;
		wroteConfig = true;
		
		Log.info("Loading MCEF config...");
		Configuration cfg = new Configuration("mcef_common.toml", ModConfig.Type.COMMON);
		//Config: main
		try {
			cfg.getBoolean("skipUpdates", "main", false, "Do not update binaries.", MCEF.class.getDeclaredField("SKIP_UPDATES"));
			cfg.getBoolean("warnUpdates", "main", true, "Tells in the chat if a new version of MCEF is available (broken).", MCEF.class.getDeclaredField("WARN_UPDATES")); // TODO: Find equavilent of playerjoinevent for fabric
			cfg.getString("cefArgs", "main", "", "Command line arguments passed to CEF. For advanced users.", (value) -> CEF_ARGS = value.split("\\s+"));
			cfg.getBoolean("shutdownJcef", "main", false, "Set this to true if your Java process hangs after closing Minecraft. This is disabled by default because it makes the launcher think Minecraft crashed...", MCEF.class.getDeclaredField("SHUTDOWN_JCEF"));
			cfg.getBoolean("secureMirrorsOnly", "main", true, "Only enable secure (HTTPS) mirror. This should be kept to true unless you know what you're doing.", MCEF.class.getDeclaredField("SECURE_MIRRORS_ONLY"));
			
			cfg.getBoolean("favorGit", "main", true,
					"Whether or not MCEF should favor downloading JCEF from a git repo. " +
							"The download process from git is faster, and will likely also be more up to date.",
					MCEF.class.getDeclaredField("FAVOR_GIT")
			);
			cfg.getString("fallbackUrlsGit", "main", "",
					"A list of URLs to fallback to.",
					(value) -> {
						if (value.isEmpty()) FALLBACK_URLS_GIT = new String[0];
						else FALLBACK_URLS_GIT = value.split("\\s+");
					}
			);
			
			cfg.getString("forcedMirror", "main", "", "A URL that contains every MCEF resources; for instance https://montoyo.net/jcef.", (value) -> {
				value = value.trim();
				if (value.length() > 0)
					FORCE_MIRROR = value;
			});
			
			//Config: exampleBrowser
			cfg.getBoolean("enable", "exampleBrowser", true, "Set this to false if you don't want to enable the F10 browser.", MCEF.class.getDeclaredField("ENABLE_EXAMPLE"));
			cfg.getString("home", "exampleBrowser", "https://google.com", "The home page of the F10 browser.", MCEF.class.getDeclaredField("HOME_PAGE"));
			
			//Config: debug
			cfg.getBoolean("checkForVRAMLeak", "debug", false, "Track allocated OpenGL textures to make sure there's no leak", MCEF.class.getDeclaredField("CHECK_VRAM_LEAK"));
			
			cfg.getBoolean("high_fps", "render", true, "If this is true, MCEF ticks at the start of each frame (Runs at the game's FPS)\nIf this is false, MCEF ticks at the start of each tick (Runs at 20 fps)", MCEF.class.getDeclaredField("HIGH_FPS"));
			cfg.getBoolean("zero_buffer", "render", true, "Zeros out the graphical information buffer before drawing to the screen; makes the game run slower, but may reduce graphical glitches", MCEF.class.getDeclaredField("ZERO_BUFFER"));
		} catch (Throwable err) {
			err.printStackTrace();
		}
		cfg.save();
	}
	
	public MCEF() {
		System.out.println("MCEF Initalizing...");
		INSTANCE = this;
		
		ensureConfig();
		
		PROXY.onPreInit();
		this.onInit(); // old init
	}
	
	public void onInit() {
		PROXY.onInit();
	}
	
	//Called by mixin
	public static void onMinecraftShutdown() {
		PROXY.onShutdown();
	}
	
}
