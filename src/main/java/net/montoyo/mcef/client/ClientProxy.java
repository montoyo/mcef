package net.montoyo.mcef.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.montoyo.mcef.BaseProxy;
import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.api.*;
import net.montoyo.mcef.api.CefClientCreationEvent;
import net.montoyo.mcef.example.ExampleMod;
import net.montoyo.mcef.utilities.CefUtil;
import net.montoyo.mcef.utilities.Log;
import net.montoyo.mcef.utilities.Util2;
import net.montoyo.mcef.virtual.VirtualBrowser;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.browser.CefBrowserOsr;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefRenderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientProxy extends BaseProxy {

    public static String ROOT = ".";
    public static String JCEF_ROOT = ".";
    public static boolean VIRTUAL = false;

    public static CefApp cefApp;
    public static CefClient cefClient;
    public static CefMessageRouter cefRouter;
    private final ArrayList<CefBrowserOsr> browsers = new ArrayList<>();
    private final ArrayList<Object> nogc = new ArrayList<>();
    public static String updateStr;
    public static final Minecraft mc = Minecraft.getInstance();
    public static final DisplayHandler displayHandler = new DisplayHandler();
    public static final HashMap<String, String> mimeTypeMap = new HashMap<>();
    public static final AppHandler appHandler = new AppHandler();
    public static ExampleMod exampleMod = new ExampleMod();

    @Override
    public void onInit() {
        super.onInit();
        MinecraftForge.EVENT_BUS.addListener(this::onTickStart);
        MinecraftForge.EVENT_BUS.addListener(this::onLogin);
        exampleMod.onInit();
    }

    public CefApp getCefApp() {
        return cefApp;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public IBrowser createBrowser(String url, boolean transp) {
        if (VIRTUAL)
            return new VirtualBrowser();

        if (cefClient == null) {
            if (cefApp == null) {
                CefSettings settings = new CefSettings();
                settings.windowless_rendering_enabled = true;
                settings.background_color = settings.new ColorType(0, 255, 255, 255);
                settings.cache_path = (new File(JCEF_ROOT, "cache")).getAbsolutePath();
                // settings.user_agent = "MCEF"

                if (CefApp.getState() == CefApp.CefAppState.NONE) {
                    CefApp.startup(MCEF.CEF_ARGS);
                }

                cefApp = CefApp.getInstance(settings);

                // Custom scheme broken on Linux, for now
                if (!OS.isLinux()) {
                    CefApp.addAppHandler(appHandler);
                }
            }
            cefClient = cefApp.createClient();
            MinecraftForge.EVENT_BUS.post(new CefClientCreationEvent(cefClient));
        }

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

    public void onTickStart(TickEvent.ClientTickEvent event) {
        if (!MCEF.HIGH_FPS)
            // listen for specifically the start of the tick
            if (event.phase == TickEvent.Phase.START)
                onFrame();
    }
    
    public void onFrame() {
        // no point in ticking CEF if it doesn't exist, or if there are no browsers
        if (cefApp == null || browsers.isEmpty()) return;
        
        if (CefUtil.isInit()) {
            mc.getProfiler().push("MCEF");

            if (cefApp != null)
                cefApp.N_DoMessageLoopWork();

            for (CefBrowserOsr b : browsers)
                b.mcefUpdate();

            displayHandler.update();
            mc.getProfiler().pop();
        }
    }

    public void onLogin(PlayerEvent.PlayerLoggedInEvent ev) {
        if (updateStr == null || !MCEF.WARN_UPDATES)
            return;

        Style cs = Style.EMPTY;
        cs.withColor(ChatFormatting.LIGHT_PURPLE);

        MutableComponent cct = (MutableComponent) Component.nullToEmpty(updateStr);
        cct.withStyle(cs);

        //ev.getPlayer().displayClientMessage(cct, true);
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

    public static void loadMimeTypeMapping() {
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

            Util2.close(br);
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
