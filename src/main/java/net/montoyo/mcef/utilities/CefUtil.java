package net.montoyo.mcef.utilities;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.api.CefInitEvent;
import net.montoyo.mcef.client.AppHandler;
import net.montoyo.mcef.client.ClientProxy;
import net.montoyo.mcef.client.DisplayHandler;
import net.montoyo.mcef.client.init.CefInitMenu;
import net.montoyo.mcef.remote.RemoteConfig;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.slf4j.Logger;

import java.io.File;

public final class CefUtil {

    private CefUtil() {
    }

    public static boolean init;
    private static boolean ready;

    @OnlyIn(Dist.CLIENT)
    public static boolean init() {
        AppHandler appHandler = ClientProxy.appHandler;
        CefApp cefApp;
        CefMessageRouter cefRouter;
        CefClient cefClient;
        String ROOT;
        String JCEF_ROOT;
        String LIBS_ROOT;
        boolean VIRTUAL = false;
        DisplayHandler displayHandler = ClientProxy.displayHandler;

        appHandler.setArgs(MCEF.CEF_ARGS);

        ROOT = ClientProxy.mc.gameDirectory.getAbsolutePath().replaceAll("\\\\", "/");

        JCEF_ROOT = ROOT + "/jcef";
        LIBS_ROOT = ROOT + "/mods/cinemamod-libraries";

        if (ROOT.endsWith("."))
            ROOT = ROOT.substring(0, ROOT.length() - 1);

        if (ROOT.endsWith("/"))
            ROOT = ROOT.substring(0, ROOT.length() - 1);

        File fileListing = new File(new File(ROOT), "config");

        RemoteConfig cfg = new RemoteConfig();
        // Forge splash used to run here
        IProgressListener ipl = CefInitMenu.listener;
    
        ipl.onProgressed(0);
        ipl.onTaskChanged("1:Load Config");
        if (!MCEF.FAVOR_GIT || !MCEF.downloadedFromGit)
            cfg.load();
        ipl.onProgressed(0.25);
    
        System.out.println("Updating MCEF file listing ");
    
        ipl.onProgressed(0.5);
        if (!cfg.updateFileListing(fileListing, false))
            Log.warning("There was a problem while establishing file list. Uninstall may not delete all files.");
    
        ipl.onProgressed(0.75);
        if (!cfg.updateFileListing(fileListing, true))
            Log.warning("There was a problem while updating file list. Uninstall may not delete all files.");
    
        ipl.onProgressed(1);
        ClientProxy.updateStr = cfg.getUpdateString();
        ipl.onProgressEnd();

        if (OS.isLinux()) {
            File[] subprocs = new File[] {
                    new File(LIBS_ROOT, "jcef_helper"),
                    new File(JCEF_ROOT, "jcef_helper")
            };
    
            boolean anyPassed = false;
            
            Throwable te = null;
            
            for (File subproc : subprocs) {
                // Attempt to make the CEF subprocess executable if not
                if (!subproc.canExecute()) {
                    try {
                        int retCode = Runtime.getRuntime().exec(new String[]{"/usr/bin/chmod", "+x", subproc.getAbsolutePath()}).waitFor();
            
                        if (retCode == 0) {
                            anyPassed = true;
                            break;
                        }
                    } catch (Throwable t) {
                        te = t;
                    }
                } else {
                    anyPassed = true;
                    break;
                }
            }
            
            if (!anyPassed) {
                if (te != null)
                    Log.errorEx("Error while giving execution rights to jcef_helper. MCEF will enter virtual mode. You can fix this by chmoding jcef_helper manually.", te);
                else
                    Log.error("Error while giving execution rights to jcef_helper. MCEF will enter virtual mode. You can fix this by chmoding jcef_helper manually.");
                VIRTUAL = true;
            }
        }

        if (VIRTUAL)
            return false;

        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = true;
        settings.background_color = settings.new ColorType(0, 255, 255, 255);
        settings.cache_path = (new File(JCEF_ROOT, "cache")).getAbsolutePath();
        // settings.user_agent = "MCEF"
    
        CefApp.startup(MCEF.CEF_ARGS);
        cefApp = CefApp.getInstance(settings);

        // Custom scheme broken on Linux, for now
        if (!OS.isLinux()) {
            CefApp.addAppHandler(appHandler);
        }

        ClientProxy.loadMimeTypeMapping();
    
        // temporarily store the cef app so that the initialization code can get it
        ClientProxy.cefApp = cefApp;
        cefClient = cefApp.createClient();
        // set it back to null so that some extra init can happen later on
        ClientProxy.cefApp = null;

        Log.info(cefApp.getVersion().toString());
        cefRouter = CefMessageRouter.create(new CefMessageRouter.CefMessageRouterConfig("mcefQuery", "mcefCancel"));
        cefClient.addMessageRouter(cefRouter);
        cefClient.addDisplayHandler(displayHandler);
        cefClient.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public boolean doClose(CefBrowser browser) {
                browser.close(true);
                return false;
            }
        });

        ClientProxy.cefApp = cefApp;
        ClientProxy.cefClient = cefClient;
        ClientProxy.cefRouter = cefRouter;

        // If shutdown patcher fail runs shutdown patcher
        // removed!

        Log.info("MCEF loaded successfuly.");
        
        init = true;
        return true;
    }

    public static boolean isInit() {
        return init;
    }
    
    public static boolean isReady() {
        return ready;
    }
    
    public static void setReady() {
        ready = true;
    }
    
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean ran = false;

    public static void runInit() {
        // TEMP HACK
        if (OS.isLinux()) {
            // https://stackoverflow.com/a/61860951
            String jvmPath = ProcessHandle.current().info().command().orElseThrow();
            jvmPath = jvmPath.replace("\\", "/");
            jvmPath = jvmPath.substring(0, jvmPath.lastIndexOf("/") - 4);
            
            System.load(jvmPath + "/lib/libjawt.so");
        }
    
        if (OS.isWindows() || OS.isLinux()) {
            if(!ran) {
                if (CefUtil.init()) {
                    MinecraftForge.EVENT_BUS.post(new CefInitEvent(true));
                    ran = true;
                    LOGGER.info("Chromium Embedded Framework initialized");
                } else {
                    ClientProxy.VIRTUAL = true;
                    
                    MinecraftForge.EVENT_BUS.post(new CefInitEvent(false));
                    ran = true;
                    LOGGER.warn("Could not initialize Chromium Embedded Framework");
                }
            }
        }
    }
}
