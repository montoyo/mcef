package net.montoyo.mcef.client;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.SplashProgress;
import net.montoyo.mcef.ShutdownPatcher;
import net.montoyo.mcef.utilities.ForgeProgressListener;
import net.montoyo.mcef.utilities.IProgressListener;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.browser.CefBrowserOsr;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.montoyo.mcef.BaseProxy;
import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.IDisplayHandler;
import net.montoyo.mcef.api.IJSQueryHandler;
import net.montoyo.mcef.example.ExampleMod;
import net.montoyo.mcef.remote.RemoteConfig;
import net.montoyo.mcef.utilities.Log;
import net.montoyo.mcef.virtual.VirtualBrowser;

public class ClientProxy extends BaseProxy {
    
    public static String ROOT = ".";
    public static boolean VIRTUAL = false;
    
    private CefApp cefApp;
    private CefClient cefClient;
    private CefMessageRouter cefRouter;
    private final ArrayList<CefBrowserOsr> browsers = new ArrayList<CefBrowserOsr>();
    private String updateStr;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final DisplayHandler displayHandler = new DisplayHandler();
    
    @Override
    public void onInit() {
        boolean enableForgeSplash = false;
        try {
            Field f = SplashProgress.class.getDeclaredField("enabled");
            f.setAccessible(true);
            enableForgeSplash = f.getBoolean(null);
        } catch(Throwable t) {
            t.printStackTrace();
        }

        ROOT = mc.mcDataDir.getAbsolutePath().replaceAll("\\\\", "/");
        if(ROOT.endsWith("."))
            ROOT = ROOT.substring(0, ROOT.length() - 1);
        
        if(ROOT.endsWith("/"))
            ROOT = ROOT.substring(0, ROOT.length() - 1);

        File fileListing = new File(new File(ROOT), "config");

        IProgressListener ipl;
        RemoteConfig cfg = new RemoteConfig();
        if(MCEF.USE_FORGE_SPLASH && enableForgeSplash)
            ipl = new ForgeProgressListener();
        else
            ipl = new UpdateFrame();
        
        cfg.load();
        if(!cfg.updateFileListing(fileListing, false))
            Log.warning("There was a problem while establishing file list. Uninstall may not delete all files.");

        if(!cfg.downloadMissing(ipl)) {
            Log.warning("Going in virtual mode; couldn't download resources.");
            VIRTUAL = true;
            return;
        }

        if(!cfg.updateFileListing(fileListing, true))
            Log.warning("There was a problem while updating file list. Uninstall may not delete all files.");
        
        updateStr = cfg.getUpdateString();
        ipl.onProgressEnd();

        if(VIRTUAL)
            return;
        
        Log.info("Now adding \"%s\" to java.library.path", ROOT);
        
        try {
            Field pathsField = ClassLoader.class.getDeclaredField("usr_paths");
            pathsField.setAccessible(true);
            
            String[] paths = (String[]) pathsField.get(null);
            String[] newList = new String[paths.length + 1];
            
            System.arraycopy(paths, 0, newList, 0, paths.length);
            newList[paths.length] = ROOT.replace('/', File.separatorChar);
            pathsField.set(null, newList);
        } catch(Exception e) {
            Log.error("Failed to do it! Entering virtual mode...");
            e.printStackTrace();
            
            VIRTUAL = true;
            return;
        }
        
        Log.info("Done without errors.");
        String exeSuffix;
        if(OS.isWindows())
            exeSuffix = ".exe";
        else
            exeSuffix = "";
        
        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = true;
        settings.background_color = settings.new ColorType(0, 255, 255, 255);
        settings.locales_dir_path = (new File(ROOT, "MCEFLocales")).getAbsolutePath();
        settings.cache_path = (new File(ROOT, "MCEFCache")).getAbsolutePath();
        settings.browser_subprocess_path = (new File(ROOT, "jcef_helper" + exeSuffix)).getAbsolutePath(); //Temporary fix
        //settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;
        
        try {
            cefApp = CefApp.getInstance(settings);
            //cefApp.myLoc = ROOT.replace('/', File.separatorChar);

            ModScheme.loadMimeTypeMapping();
            CefApp.addAppHandler(new AppHandler());
            cefClient = cefApp.createClient();
        } catch(Throwable t) {
            Log.error("Going in virtual mode; couldn't initialize CEF.");
            t.printStackTrace();
            
            VIRTUAL = true;
            return;
        }
        
        Log.info(cefApp.getVersion().toString());
        cefRouter = CefMessageRouter.create(new CefMessageRouterConfig("mcefQuery", "mcefCancel"));
        cefClient.addMessageRouter(cefRouter);
        cefClient.addDisplayHandler(displayHandler);

        if(!ShutdownPatcher.didPatchSucceed()) {
            Log.warning("ShutdownPatcher failed to patch Minecraft.run() method; starting ShutdownThread...");
            (new ShutdownThread()).start();
        }

        MinecraftForge.EVENT_BUS.register(this);
        if(MCEF.ENABLE_EXAMPLE)
            (new ExampleMod()).onInit();
        
        Log.info("MCEF loaded successfuly.");
    }
    
    public CefApp getCefApp() {
        return cefApp;
    }
    
    @Override
    public IBrowser createBrowser(String url, boolean transp) {
        if(VIRTUAL)
            return new VirtualBrowser();
        
        CefBrowserOsr ret = (CefBrowserOsr) cefClient.createBrowser(url, true, transp);
        browsers.add(ret);
        return ret;
    }
    
    @Override
    public void registerDisplayHandler(IDisplayHandler idh) {
        displayHandler.addHandler(idh);
    }
    
    @Override
    public boolean isVirtual() {
        return VIRTUAL;
    }
    
    @Override
    public void openExampleBrowser(String url) {
        if(MCEF.ENABLE_EXAMPLE)
            ExampleMod.INSTANCE.showScreen(url);
    }
    
    @Override
    public void registerJSQueryHandler(IJSQueryHandler iqh) {
        if(!VIRTUAL)
            cefRouter.addHandler(new MessageRouter(iqh), false);
    }
    
    @SubscribeEvent
    public void onTick(TickEvent.RenderTickEvent ev) {
        if(ev.phase == TickEvent.Phase.START) {
            mc.mcProfiler.startSection("MCEF");
            
            for(CefBrowserOsr b: browsers)
                b.mcefUpdate();

            displayHandler.update();
            mc.mcProfiler.endSection();
        }
    }
    
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent ev) {
        if(updateStr == null || !MCEF.WARN_UPDATES)
            return;
        
        Style cs = new Style();
        cs.setColor(TextFormatting.LIGHT_PURPLE);
        
        TextComponentString cct = new TextComponentString(updateStr);
        cct.setStyle(cs);
        
        ev.player.sendMessage(cct);
    }
    
    public void removeBrowser(CefBrowserOsr b) {
        browsers.remove(b);
    }

    @Override
    public IBrowser createBrowser(String url) {
        return createBrowser(url, false);
    }

    @Override
    public void onShutdown() {
        if(VIRTUAL)
            return;

        Log.info("Shutting down JCEF...");
        CefBrowserOsr.CLEANUP = false; //Workaround

        for(CefBrowserOsr b: browsers)
            b.close();

        browsers.clear();
        cefClient.dispose();

        try {
            //Yea sometimes, this is needed for some reasons.
            Thread.sleep(100);
        } catch(Throwable t) {}

        cefApp.N_Shutdown();
    }

}
