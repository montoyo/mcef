package net.montoyo.mcef.client;

import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.api.IScheme;
import net.montoyo.mcef.utilities.Log;

import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

import java.util.HashMap;
import java.util.Map;

public class AppHandler extends CefAppHandlerAdapter {

    public AppHandler() {
        super(new String[] {});
    }

    private static class SchemeData {

        private Class<? extends IScheme> cls;
        private boolean std;
        private boolean local;
        private boolean dispIsolated;
        private boolean secure;
        private boolean corsEnabled;
        private boolean cspBypassing;
        private boolean fetchEnabled;

        private SchemeData(Class<? extends IScheme> cls, boolean std, boolean local, boolean dispIsolated, boolean secure, boolean corsEnabled, boolean cspBypassing, boolean fetchEnabled) {
            this.cls = cls;
            this.std = std;
            this.local = local;
            this.dispIsolated = dispIsolated;
            this.secure = secure;
            this.corsEnabled = corsEnabled;
            this.cspBypassing = cspBypassing;
            this.fetchEnabled = fetchEnabled;
        }

    }

    private final HashMap<String, SchemeData> schemeMap = new HashMap<>();

    public void registerScheme(String name, Class<? extends IScheme> cls, boolean std, boolean local, boolean dispIsolated, boolean secure, boolean corsEnabled, boolean cspBypassing, boolean fetchEnabled) {
        schemeMap.put(name, new SchemeData(cls, std, local, dispIsolated, secure, corsEnabled, cspBypassing, fetchEnabled));
    }

    public boolean isSchemeRegistered(String name) {
        return schemeMap.containsKey(name);
    }
    
    @Override
    public void onRegisterCustomSchemes(CefSchemeRegistrar reg) {
        int cnt = 0;

        for(Map.Entry<String, SchemeData> entry : schemeMap.entrySet()) {
            SchemeData v = entry.getValue();

            if(reg.addCustomScheme(entry.getKey(), v.std, v.local, v.dispIsolated, v.secure, v.corsEnabled, v.cspBypassing, v.fetchEnabled))
                cnt++;
            else
                Log.error("Could not register scheme %s", entry.getKey());
        }

        Log.info("%d schemes registered", cnt);
    }
    
    @Override
    public void onContextInitialized() {
        CefApp app = ((ClientProxy) MCEF.PROXY).getCefApp();

        for(Map.Entry<String, SchemeData> entry : schemeMap.entrySet())
            app.registerSchemeHandlerFactory(entry.getKey(), "", new SchemeHandlerFactory(entry.getValue().cls));
    }
    
    private static class SchemeHandlerFactory implements CefSchemeHandlerFactory {

        private Class<? extends IScheme> cls;

        private SchemeHandlerFactory(Class<? extends IScheme> cls) {
            this.cls = cls;
        }

        @Override
        public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
            try {
                return new SchemeResourceHandler(cls.newInstance());
            } catch(Throwable t) {
                t.printStackTrace();
                return null;
            }
        }
        
    }

}
