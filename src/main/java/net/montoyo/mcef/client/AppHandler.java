package net.montoyo.mcef.client;

import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.api.IScheme;
import net.montoyo.mcef.utilities.Log;

import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefCallback;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.BoolRef;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

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
    
    boolean registered = false;

    private final HashMap<String, SchemeData> schemeMap = new HashMap<>();

    public void registerScheme(String name, Class<? extends IScheme> cls, boolean std, boolean local, boolean dispIsolated, boolean secure, boolean corsEnabled, boolean cspBypassing, boolean fetchEnabled) {
        schemeMap.put(name, new SchemeData(cls, std, local, dispIsolated, secure, corsEnabled, cspBypassing, fetchEnabled));
        
        if (registered) {
            CefApp app = CefApp.getInstance();
            app.registerSchemeHandlerFactory(name, "", new SchemeHandlerFactory(schemeMap.get(name).cls));
        }
    }

    public boolean isSchemeRegistered(String name) {
        return schemeMap.containsKey(name);
    }
    
    @Override
    public void onRegisterCustomSchemes(CefSchemeRegistrar reg) {
        registered = true;
        
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
    
        private static final ClassLoader clr;
    
        static {
            // this may look odd, but it makes CEF load a *lot* faster, and also makes it not spam errors to console
            ClassLoader c = Thread.currentThread().getContextClassLoader();
            if (c == null) c = SchemeResourceHandler.class.getClassLoader();
            clr = c;
    
            try {
                //noinspection unused
                Class<?>[] LOADER = new Class[] {
                        IntRef.class,
                        BoolRef.class,
                        CefRequest.class,
                        StringRef.class,
                        SchemeResponseData.class,
                        SchemeResponseHeaders.class,
                        Class.forName("org.cef.callback.CefCallback_N"),
                        Class.forName("org.cef.network.CefResponse_N")
                };
            } catch (Throwable err) {
                err.printStackTrace();
            }
        }
        
        private final Class<? extends IScheme> cls;

        private SchemeHandlerFactory(Class<? extends IScheme> cls) {
            this.cls = cls;
        }

        @Override
        public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
            Thread.currentThread().setContextClassLoader(clr);

            try {
                return new SchemeResourceHandler(cls.newInstance());
            } catch(Throwable t) {
                t.printStackTrace();
                return null;
            }
        }
        
    }

}
