package net.montoyo.mcef.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.SplashProgress;
import net.montoyo.mcef.coremod.ShutdownPatcher;
import net.montoyo.mcef.api.IScheme;
import net.montoyo.mcef.utilities.ForgeProgressListener;
import net.montoyo.mcef.utilities.IProgressListener;
import net.montoyo.mcef.utilities.Util;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.browser.CefBrowser;
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
import org.cef.browser.CefRenderer;
import org.cef.handler.CefLifeSpanHandlerAdapter;

import javax.swing.*;

public class ClientProxy extends BaseProxy {
    
    public static String ROOT = ".";
    public static boolean VIRTUAL = false;
    
    private CefApp cefApp;
    private CefClient cefClient;
    private CefMessageRouter cefRouter;
    private final ArrayList<CefBrowserOsr> browsers = new ArrayList<>();
    private String updateStr;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final DisplayHandler displayHandler = new DisplayHandler();
    private final HashMap<String, String> mimeTypeMap = new HashMap<>();
    private final AppHandler appHandler = new AppHandler();
    private ExampleMod exampleMod;

    public static final String LINUX_WIKI = "https://montoyo.net/wdwiki/Linux";

    @Override
    public void onPreInit() {
        exampleMod = new ExampleMod();
        exampleMod.onPreInit(); //Do it even if example mod is disabled because it registers the "mod://" scheme
    }

    @Override
    public void onInit() {
        appHandler.setArgs(MCEF.CEF_ARGS);

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
        File[] resourceArray = cfg.getResourceArray();

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
            
            System.arraycopy(paths, 0, newList, 1, paths.length);
            newList[0] = ROOT.replace('/', File.separatorChar);
            pathsField.set(null, newList);
        } catch(Exception e) {
            Log.error("Failed to do it! Entering virtual mode...");
            e.printStackTrace();
            
            VIRTUAL = true;
            return;
        }
        
        Log.info("Done without errors.");

        if(OS.isLinux()) {
            //LinuxPatch.doPatch(resourceArray); //Not needed, from what I experienced...

            FileSystem fs = FileSystems.getDefault();
            Path here = fs.getPath(mc.mcDataDir.getPath());
            String[] libPath = Util.getenv("LD_LIBRARY_PATH").split(":");

            if(Arrays.stream(libPath).filter(s -> !s.isEmpty()).map(fs::getPath).noneMatch(p -> Util.isSameFile(p, here))) {
                Log.error("On Linux, you *HAVE* to add the .minecraft folder to LD_LIBRARY_PATH in order for MCEF to work.");
                Log.error("You can do this by running the following command and then starting Minecraft within the same terminal:");
                Log.error("export \"LD_LIBRARY_PATH=$LD_LIBRARY_PATH:%s\"", ROOT);
                Log.error("");
                Log.error("Since this has not been done yet, MCEF will now enter virtual mode and WILL NOT WORK.");
                Log.error("For more info, please read %s", LINUX_WIKI);
                Log.error("Please don't post a GitHub issue for this.");

                int ans = JOptionPane.showConfirmDialog(null, "A bug on Linux requires you to add the Minecraft folder to LD_LIBRARY_PATH.\nThis has not been done, so MCEF will not work for now.\nWould you like to open the wiki page?",
                        "MCEF Linux", JOptionPane.YES_NO_OPTION);

                if(ans == JOptionPane.YES_OPTION) {
                    try {
                        Runtime.getRuntime().exec("xdg-open " + LINUX_WIKI);
                    } catch(IOException ex) {
                        Log.errorEx("Could not open wiki page", ex);
                        JOptionPane.showMessageDialog(null, "Couldn't automatically open the wiki page. The link is:\n" + LINUX_WIKI, "MCEF Linux", JOptionPane.ERROR_MESSAGE);
                    }
                }

                VIRTUAL = true;
                return;
            }
        }

        String exeSuffix;
        if(OS.isWindows())
            exeSuffix = ".exe";
        else
            exeSuffix = "";

        File subproc = new File(ROOT, "jcef_helper" + exeSuffix);
        if(OS.isLinux() && !subproc.canExecute()) {
            try {
                int retCode = Runtime.getRuntime().exec(new String[] { "/usr/bin/chmod", "+x", subproc.getAbsolutePath() }).waitFor();

                if(retCode != 0)
                    throw new RuntimeException("chmod exited with code " + retCode);
            } catch(Throwable t) {
                Log.errorEx("Error while giving execution rights to jcef_helper. MCEF will probably enter virtual mode. You can fix this by chmoding jcef_helper manually.", t);
            }
        }
        
        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = true;
        settings.background_color = settings.new ColorType(0, 255, 255, 255);
        settings.locales_dir_path = (new File(ROOT, "MCEFLocales")).getAbsolutePath();
        settings.cache_path = (new File(ROOT, "MCEFCache")).getAbsolutePath();
        settings.browser_subprocess_path = subproc.getAbsolutePath();
        //settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;
        
        try {
            ArrayList<String> libs = new ArrayList<>();

            if(OS.isWindows()) {
                libs.add("d3dcompiler_47.dll");
                libs.add("libGLESv2.dll");
                libs.add("libEGL.dll");
                libs.add("chrome_elf.dll");
                libs.add("libcef.dll");
                libs.add("jcef.dll");
            } else {
                libs.add("libcef.so");
                libs.add("libjcef.so");
            }

            for(String lib: libs) {
                File f = new File(ROOT, lib);
                try {
                    f = f.getCanonicalFile();
                } catch(IOException ex) {
                    f = f.getAbsoluteFile();
                }

                System.load(f.getPath());
            }

            CefApp.startup();
            cefApp = CefApp.getInstance(settings);
            //cefApp.myLoc = ROOT.replace('/', File.separatorChar);

            loadMimeTypeMapping();
            CefApp.addAppHandler(appHandler);
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
        cefClient.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public boolean doClose(CefBrowser browser) {
                browser.close(true);
                return false;
            }
        });

        if(!ShutdownPatcher.didPatchSucceed()) {
            Log.warning("ShutdownPatcher failed to patch Minecraft.run() method; starting ShutdownThread...");
            (new ShutdownThread()).start();
        }

        MinecraftForge.EVENT_BUS.register(this);
        if(MCEF.ENABLE_EXAMPLE)
            exampleMod.onInit();
        
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
        ret.setCloseAllowed();
        ret.createImmediately();

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
            exampleMod.showScreen(url);
    }
    
    @Override
    public void registerJSQueryHandler(IJSQueryHandler iqh) {
        if(!VIRTUAL)
            cefRouter.addHandler(new MessageRouter(iqh), false);
    }

    @Override
    public void registerScheme(String name, Class<? extends IScheme> schemeClass, boolean std, boolean local, boolean displayIsolated, boolean secure, boolean corsEnabled, boolean cspBypassing, boolean fetchEnabled) {
        appHandler.registerScheme(name, schemeClass, std, local, displayIsolated, secure, corsEnabled, cspBypassing, fetchEnabled);
    }

    @Override
    public boolean isSchemeRegistered(String name) {
        return appHandler.isSchemeRegistered(name);
    }

    @SubscribeEvent
    public void onTick(TickEvent.RenderTickEvent ev) {
        if(ev.phase == TickEvent.Phase.START) {
            mc.mcProfiler.startSection("MCEF");

            if(cefApp != null)
                cefApp.N_DoMessageLoopWork();

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

    private void runMessageLoopFor(long ms) {
        final long start = System.currentTimeMillis();

        do {
            cefApp.N_DoMessageLoopWork();
        } while(System.currentTimeMillis() - start < ms);
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

        if(MCEF.CHECK_VRAM_LEAK)
            CefRenderer.dumpVRAMLeak();

        runMessageLoopFor(100);
        CefApp.forceShutdownState();
        cefClient.dispose();

        if(MCEF.SHUTDOWN_JCEF)
            cefApp.N_Shutdown();
    }

    public void loadMimeTypeMapping() {
        Pattern p = Pattern.compile("^(\\S+)\\s+(\\S+)\\s*(\\S*)\\s*(\\S*)$");
        String line = "";
        int cLine = 0;
        mimeTypeMap.clear();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(ClientProxy.class.getResourceAsStream("/assets/mcef/mime.types")));

            while(true) {
                cLine++;
                line = br.readLine();
                if(line == null)
                    break;

                line = line.trim();
                if(!line.startsWith("#")) {
                    Matcher m = p.matcher(line);
                    if(!m.matches())
                        continue;

                    mimeTypeMap.put(m.group(2), m.group(1));
                    if(m.groupCount() >= 4 && !m.group(3).isEmpty()) {
                        mimeTypeMap.put(m.group(3), m.group(1));

                        if(m.groupCount() >= 5 && !m.group(4).isEmpty())
                            mimeTypeMap.put(m.group(4), m.group(1));
                    }
                }
            }

            Util.close(br);
        } catch(Throwable e) {
            Log.error("[Mime Types] Error while parsing \"%s\" at line %d:", line, cLine);
            e.printStackTrace();
        }

        Log.info("Loaded %d mime types", mimeTypeMap.size());
    }

    @Override
    public String mimeTypeFromExtension(String ext) {
        ext = ext.toLowerCase();
        String ret = mimeTypeMap.get(ext);
        if(ret != null)
            return ret;

        //If the mimeTypeMap couldn't be loaded, fall back to common things
        switch(ext) {
            case "htm":
            case "html":
                return "text/html";

            case "css":
                return "text/css";

            case "js":
                return "text/javascript";

            case "png":
                return "image/png";

            case "jpg":
            case "jpeg":
                return "image/jpeg";

            case "gif":
                return "image/gif";

            case "svg":
                return "image/svg+xml";

            case "xml":
                return "text/xml";

            case "txt":
                return "text/plain";

            default:
                return null;
        }
    }
}
