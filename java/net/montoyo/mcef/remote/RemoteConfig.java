package net.montoyo.mcef.remote;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;

import org.cef.OS;

import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.client.ClientProxy;
import net.montoyo.mcef.utilities.IProgressListener;
import net.montoyo.mcef.utilities.Log;
import net.montoyo.mcef.utilities.Util;
import net.montoyo.mcef.utilities.Version;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A class for updating and parsing the remote configuration file.
 * @author montoyo
 *
 */
public class RemoteConfig {
	
	private static String PLATFORM;
	private ResourceList resources = new ResourceList();
	private ArrayList<String> extract = new ArrayList<String>();
	private String version = null;
	
	public RemoteConfig() {
	}
	
	/**
	 * Parses the MCEF configuration file.
	 * 
	 * @param f The configuration file.
	 * @return The parsed configuration file.
	 */
	private JsonObject readConfig(File f) {
		try {
			return (new JsonParser()).parse(new FileReader(f)).getAsJsonObject();
		} catch(JsonIOException e) {
			Log.error("IOException while reading remote config.");
			e.printStackTrace();
			return null;
		} catch(FileNotFoundException e) {
			Log.error("Couldn't find remote config.");
			e.printStackTrace();
			return null;
		} catch(Exception e) {
			Log.error("Syntax error in remote config.");
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Updates the MCEF configuration file and parses it.
	 * @return The parsed configuration file.
	 */
	private JsonObject readConfig() {
		File newCfg = new File(ClientProxy.ROOT, "mcef.new");
		File cfgFle = new File(ClientProxy.ROOT, "mcef.json");
		
		boolean ok = Util.download("config.json", newCfg, null);
		Mirror.reset(); //We may reset mirrors state because config.json is a special case.
		
		if(ok) {
			Util.delete(cfgFle);
			
			if(newCfg.renameTo(cfgFle))
				return readConfig(cfgFle);
			else {
				Log.warning("Couldn't rename mcef.new to mcef.json.");
				return readConfig(newCfg);
			}
			
		} else {
			Log.warning("Couldn't read remote config. Using local configuration file.");
			return readConfig(cfgFle);
		}
	}
	
	/**
	 * Updates the MCEF configuration file and parses it.
	 * Fills the resources, extract and version fields from it.
	 */
	public void load() {
		JsonObject cfg = readConfig();
		if(cfg == null) {
			Log.error("Could NOT read either remote and local configuration files. Entering virtual mode.");
			ClientProxy.VIRTUAL = true;
			return;
		}
		
		String id;
		if(OS.isWindows())
			id = "win";
		else if(OS.isMacintosh())
			id = "mac";
		else if(OS.isLinux())
			id = "linux";
		else {
			//Shouldn't happen.
			Log.error("Your OS isn't supported by MCEF. Entering virtual mode.");
			ClientProxy.VIRTUAL = true;
			return;
		}
		
		String arch = System.getProperty("sun.arch.data.model");
		if(!arch.equals("32") && !arch.equals("64")) {
			//Shouldn't happen.
			Log.error("Your CPU arch isn't supported by MCEF. Entering virtual mode.");
			ClientProxy.VIRTUAL = true;
			return;
		}
		
		PLATFORM = id + arch;
		Log.info("Detected platform: %s", PLATFORM);
		
		JsonElement cat = cfg.get("platforms");
		if(cat == null || !cat.isJsonObject()) {
			Log.error("Config file is missing \"platforms\" object. Entering virtual mode.");
			ClientProxy.VIRTUAL = true;
			return;
		}
		
		JsonElement res = cat.getAsJsonObject().get(PLATFORM);
		if(res == null || !res.isJsonObject()) {
			Log.error("Your platform isn't supported by MCEF yet. Entering virtual mode.");
			ClientProxy.VIRTUAL = true;
			return;
		}
		
		resources.clear();
		Set<Entry<String, JsonElement>> files = res.getAsJsonObject().entrySet();
		
		for(Entry<String, JsonElement> e: files) {
			if(e.getValue() == null || !e.getValue().isJsonPrimitive())
				continue;
			
			resources.add(new Resource(e.getKey(), e.getValue().getAsString()));
		}
		
		JsonElement ext = cfg.get("extract");
		if(ext != null && ext.isJsonArray()) {
			JsonArray ray = ext.getAsJsonArray();
			
			for(JsonElement e: ray) {
				if(e != null && e.isJsonPrimitive())
					extract.add(e.getAsString());
			}
		}
		
		JsonElement ver = cfg.get("version");
		if(ver != null && ver.isJsonPrimitive())
			version = ver.getAsString();
	}
	
	/**
	 * Detects missing files, download them, and if needed, extracts them.
	 * 
	 * @param ipl The progress listener.
	 * @return true if the operation was successful.
	 */
	public boolean downloadMissing(IProgressListener ipl) {
		Log.info("Checking for missing resources...");
		resources.removeExistings();
		
		if(resources.size() > 0) {
			Log.info("Found %d missing resources. Downloading...", resources.size());
			for(Resource r: resources) {
				if(!r.download(ipl, PLATFORM))
					return false;
			}
			
			for(String r: extract) {
				Resource res = resources.fromFileName(r);
				if(res == null) //Not missing; no need to extract
					continue;
				
				if(!res.extract(ipl)) //Probably not a huge problem if we can't extract some resources... no need to return.
					Log.warning("Couldn't extract %s. MCEF may not work because of this.", r);
			}
			
			Log.info("Done; all resources were downloaded.");
		} else
			Log.info("None are missing. Good.");
		
		return true;
	}
	
	/**
	 * Returns an info string if an MCEF update is available.
	 * @return an info string if a newer version exists, null otherwise.
	 */
	public String getUpdateString() {
		if(version == null)
			return null;
		
		Version cur = new Version(MCEF.VERSION);
		Version cfg = new Version(version);
		
		if(cfg.isBiggerThan(cur))
			return "New MCEF version available. Current: " + cur + ", latest: " + cfg + '.';
		
		return null;
	}

}
