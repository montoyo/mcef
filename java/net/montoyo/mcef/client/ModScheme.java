package net.montoyo.mcef.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.montoyo.mcef.utilities.Log;
import net.montoyo.mcef.utilities.Util;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

public class ModScheme extends CefResourceHandlerAdapter {

	private static HashMap<String, String> mimeTypeMap;
	private String contentType = null;
	private InputStream is = null;

    public static void loadMimeTypeMapping() {
        Pattern p = Pattern.compile("^(\\S+)\\s+(\\S+)\\s*(\\S*)\\s*(\\S*)$");
        String line = "";
        int cLine = 0;
        mimeTypeMap = new HashMap<String, String>();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(ModScheme.class.getResourceAsStream("/assets/mcef/mime.types")));

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
            String ext = loc.substring(pos + 1).toLowerCase();

            if(mimeTypeMap != null && mimeTypeMap.containsKey(ext))
                contentType = mimeTypeMap.get(ext);
            else if(ext.equals("html"))
                contentType = "text/html";
            else if(ext.equals("css"))
                contentType = "text/css";
            else if(ext.equals("js"))
                contentType = "text/javascript";
            else if(ext.equals("png"))
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
