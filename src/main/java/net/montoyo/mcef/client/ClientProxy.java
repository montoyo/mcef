package net.montoyo.mcef.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.montoyo.mcef.BaseProxy;
import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.IDisplayHandler;
import net.montoyo.mcef.api.IJSQueryHandler;
import net.montoyo.mcef.api.IScheme;
import net.montoyo.mcef.example.ExampleMod;
import net.montoyo.mcef.remote.RemoteConfig;
import net.montoyo.mcef.utilities.IProgressListener;
import net.montoyo.mcef.utilities.Log;
import net.montoyo.mcef.utilities.Util;
import net.montoyo.mcef.virtual.VirtualBrowser;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.browser.*;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.handler.CefLifeSpanHandlerAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientProxy extends BaseProxy {

    public static String ROOT = ".";
    public static String JCEF_ROOT = ".";
    public static boolean VIRTUAL = false;

    private CefApp cefApp;
    private CefClient cefClient;
    private CefMessageRouter cefRouter;
    private final ArrayList<CefBrowserOsr> browsers = new ArrayList<>();
    private final ArrayList<Object> nogc = new ArrayList<>();
    private String updateStr;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final DisplayHandler displayHandler = new DisplayHandler();
    private final HashMap<String, String> mimeTypeMap = new HashMap<>();
    private final AppHandler appHandler = new AppHandler();
    private ExampleMod exampleMod;

    @Override
    public void onPreInit() {
        exampleMod = new ExampleMod();
        exampleMod.onPreInit(); //Do it even if example mod is disabled because it registers the "mod://" scheme
    }

    @Override
    public void onInit() {
        appHandler.setArgs(MCEF.CEF_ARGS);

        ROOT = mc.runDirectory.getAbsolutePath().replaceAll("\\\\", "/");
        if (ROOT.endsWith("."))
            ROOT = ROOT.substring(0, ROOT.length() - 1);

        if (ROOT.endsWith("/"))
            ROOT = ROOT.substring(0, ROOT.length() - 1);

        JCEF_ROOT = ROOT + "/jcef";

        File fileListing = new File(new File(ROOT), "config");

        IProgressListener ipl;
        RemoteConfig cfg = new RemoteConfig();
        // Forge splash used to run here
        System.out.println("SYSTEM HEADLESS PROPERTY: " + System.getProperty("java.awt.headless"));
        System.setProperty("java.awt.headless","false"); // local is bugged for me
        ipl = new UpdateFrame();

        cfg.load();

        System.out.println("Updating MCEF file listing ");

        if (!cfg.updateFileListing(fileListing, false))
            Log.warning("There was a problem while establishing file list. Uninstall may not delete all files.");

        System.out.println("Updating MCEF missing files... ");

        if (!cfg.downloadMissing(ipl)) {
            Log.warning("Going in virtual mode; couldn't download resources.");
            VIRTUAL = true;
            return;
        }

        if (!cfg.updateFileListing(fileListing, true))
            Log.warning("There was a problem while updating file list. Uninstall may not delete all files.");

        updateStr = cfg.getUpdateString();
        ipl.onProgressEnd();

        if (OS.isLinux()) {
            File subproc = new File(JCEF_ROOT, "jcef_helper");

            // Attempt to make the CEF subprocess executable if not
            if (!subproc.canExecute()) {
                try {
                    int retCode = Runtime.getRuntime().exec(new String[]{"/usr/bin/chmod", "+x", subproc.getAbsolutePath()}).waitFor();

                    if (retCode != 0)
                        throw new RuntimeException("chmod exited with code " + retCode);
                } catch (Throwable t) {
                    Log.errorEx("Error while giving execution rights to jcef_helper. MCEF will enter virtual mode. You can fix this by chmoding jcef_helper manually.", t);
                    VIRTUAL = true;
                }
            }
        }

        if (VIRTUAL)
            return;

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

        loadMimeTypeMapping();

        cefClient = cefApp.createClient();

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

        // If shutdown patcher fail runs shutdown patcher
        // removed!

        if (MCEF.ENABLE_EXAMPLE)
            exampleMod.onInit();

        Log.info("MCEF loaded successfuly.");
    }

    public CefApp getCefApp() {
        return cefApp;
    }

    @Override
    public IBrowser createBrowser(String url, boolean transp) {
        if (VIRTUAL)
            return new VirtualBrowser();

        System.out.println("Creating CEF browser at url " + url);

        CefBrowserOsr ret = (CefBrowserOsr) cefClient.createBrowser(url, true, transp);
        ret.setCloseAllowed();
        ret.createImmediately();
        ret.loadURL("http://localhost:8181");

        /*CefBrowserWr ret2 = (CefBrowserWr) cefClient.createBrowser(url, false, transp);
        ret2.setCloseAllowed();
        ret2.createImmediately();
        ret2.loadURL("http://localhost:8181");

        nogc.add(ret2);*/
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
        if (MCEF.ENABLE_EXAMPLE)
            exampleMod.showScreen(url);
    }

    @Override
    public void registerJSQueryHandler(IJSQueryHandler iqh) {
        if (!VIRTUAL)
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
    public void onTickStart(TickEvent.ClientTickEvent event) {
        mc.getProfiler().push("MCEF");

        if (cefApp != null)
            cefApp.N_DoMessageLoopWork();

        for (CefBrowserOsr b : browsers)
            b.mcefUpdate();

        displayHandler.update();
        mc.getProfiler().pop();
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent ev) {
        if (updateStr == null || !MCEF.WARN_UPDATES)
            return;

        Style cs = Style.EMPTY;
        cs.withColor(Formatting.LIGHT_PURPLE);

        MutableText cct = (MutableText) Text.of(updateStr);
        cct.getWithStyle(cs);

        ev.getPlayer().sendMessage(cct, true);
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
        } while (System.currentTimeMillis() - start < ms);
    }

    @Override
    public void onShutdown() {
        if (VIRTUAL)
            return;

        Log.info("Shutting down JCEF...");
        CefBrowserOsr.CLEANUP = false; //Workaround

        for (CefBrowserOsr b : browsers)
            b.close();

        browsers.clear();

        if (MCEF.CHECK_VRAM_LEAK)
            CefRenderer.dumpVRAMLeak();

        runMessageLoopFor(100);
        CefApp.forceShutdownState();
        cefClient.dispose();

        if (MCEF.SHUTDOWN_JCEF)
            cefApp.N_Shutdown();
    }

    public void loadMimeTypeMapping() {
        Pattern p = Pattern.compile("^(\\S+)\\s+(\\S+)\\s*(\\S*)\\s*(\\S*)$");
        String line = "";
        int cLine = 0;
        mimeTypeMap.clear();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(ClientProxy.class.getResourceAsStream("/assets/mcef/mime.types")));

            while (true) {
                cLine++;
                line = br.readLine();
                if (line == null)
                    break;

                line = line.trim();
                if (!line.startsWith("#")) {
                    Matcher m = p.matcher(line);
                    if (!m.matches())
                        continue;

                    mimeTypeMap.put(m.group(2), m.group(1));
                    if (m.groupCount() >= 4 && !m.group(3).isEmpty()) {
                        mimeTypeMap.put(m.group(3), m.group(1));

                        if (m.groupCount() >= 5 && !m.group(4).isEmpty())
                            mimeTypeMap.put(m.group(4), m.group(1));
                    }
                }
            }

            Util.close(br);
        } catch (Throwable e) {
            Log.error("[Mime Types] Error while parsing \"%s\" at line %d:", line, cLine);
            e.printStackTrace();
        }

        Log.info("Loaded %d mime types", mimeTypeMap.size());
    }

    @Override
    public String mimeTypeFromExtension(String ext) {
        ext = ext.toLowerCase();
        String ret = mimeTypeMap.get(ext);
        if (ret != null)
            return ret;

        //If the mimeTypeMap couldn't be loaded, fall back to common things
        switch (ext) {
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
