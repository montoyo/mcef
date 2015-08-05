package net.montoyo.mcef.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import net.montoyo.mcef.utilities.Log;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

public class ModScheme extends CefResourceHandlerAdapter {
	
	private String contentType = null;
	private InputStream is = null;
	
	@Override
	public synchronized boolean processRequest(CefRequest req, CefCallback cb) {
		String url = req.getURL().substring("mod://".length());
		
		int pos = url.indexOf('/');
		if(pos < 0)
			return false;
		
		String mod = removeSlashes(url.substring(0, pos));
		String loc = removeSlashes(url.substring(pos + 1));

		if(mod.length() <= 0 || loc.length() <= 0 || mod.charAt(0) == '.' || loc.charAt(0) == '.') {
            Log.warning("Invalid URL " + req.getURL());
            return false;
        }
		
		is = ModScheme.class.getResourceAsStream("/assets/" + mod.toLowerCase() + "/html/" + loc.toLowerCase());
		if(is == null) {
            Log.warning("Resource " + req.getURL() + " NOT found!");
            return false; //Mhhhhh... 404?
        }

        contentType = null;
        pos = loc.lastIndexOf('.');
        if(pos >= 0 && pos < loc.length() - 2) {
            String ext = loc.substring(pos + 1);

            if(ext.equalsIgnoreCase("html"))
                contentType = "text/html";
            else if(ext.equalsIgnoreCase("css"))
                contentType = "text/css";
            else if(ext.equalsIgnoreCase("js"))
                contentType = "text/javascript";
            else if(ext.equalsIgnoreCase("png"))
                contentType = "image/png";
        }

		cb.Continue();
		return true;
	}
	
	private String removeSlashes(String loc) {
		while(loc.length() > 0 && loc.charAt(0) == '/')
			loc = loc.substring(1);
		
		return loc;
	}
	
	@Override
	public void getResponseHeaders(CefResponse rep, IntRef response_length, StringRef redirectUrl) {
        if(contentType != null)
		    rep.setMimeType(contentType);

		rep.setStatus(200);
        rep.setStatusText("OK");
		response_length.set(-1);
	}
	
	@Override
	public synchronized boolean readResponse(byte[] out, int toRead, IntRef acRead, CefCallback callback) {
		try {
			int ret = is.read(out, 0, toRead);
			if(ret <= 0)
				is.close();
			
			acRead.set(Math.max(ret, 0));
			return ret > 0;
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		}
	}

}
