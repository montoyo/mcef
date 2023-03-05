package net.montoyo.mcef.utilities;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.client.AppHandler;
import net.montoyo.mcef.client.ClientProxy;
import net.montoyo.mcef.client.DisplayHandler;
import net.montoyo.mcef.client.UpdateFrame;
import net.montoyo.mcef.remote.RemoteConfig;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefLifeSpanHandlerAdapter;

import java.io.File;

public final class CefUtil {

    private CefUtil() {
    }

    public static boolean init;

    @OnlyIn(Dist.CLIENT)
    public static boolean init(ClientProxy clientProxy) {
        AppHandler appHandler = clientProxy.appHandler;
        CefApp cefApp;
        CefMessageRouter cefRouter;
        CefClient cefClient;
        String ROOT;
        String JCEF_ROOT;
        boolean VIRTUAL = false;
        DisplayHandler displayHandler = clientProxy.displayHandler;

        appHandler.setArgs(MCEF.CEF_ARGS);

        ROOT = System.getProperty("user.dir").replaceAll("\\\\", "/");

        System.out.println(ROOT);

        JCEF_ROOT = ROOT + "/jcef";

        if (ROOT.endsWith("."))
            ROOT = ROOT.substring(0, ROOT.length() - 1);

        if (ROOT.endsWith("/"))
            ROOT = ROOT.substring(0, ROOT.length() - 1);

        File fileListing = new File(new File(ROOT), "config");

        IProgressListener ipl;
        RemoteConfig cfg = new RemoteConfig();
        // Forge splash used to run here
        System.out.println("SYSTEM HEADLESS PROPERTY: " + System.getProperty("java.awt.headless"));
        System.setProperty("java.awt.headless", "false"); // local is bugged for me
        ipl = new UpdateFrame();

        cfg.load();

        System.out.println("Updating MCEF file listing ");

        if (!cfg.updateFileListing(fileListing, false))
            Log.warning("There was a problem while establishing file list. Uninstall may not delete all files.");

        if (!cfg.updateFileListing(fileListing, true))
            Log.warning("There was a problem while updating file list. Uninstall may not delete all files.");

        clientProxy.updateStr = cfg.getUpdateString();
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

        //if (VIRTUAL)
          //  return false;

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

        clientProxy.loadMimeTypeMapping();

        cefClient = cefApp.createClient();

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
}
