package net.montoyo.mcef.remote;

import java.io.File;

import net.montoyo.mcef.client.ClientProxy;
import net.montoyo.mcef.utilities.IProgressListener;
import net.montoyo.mcef.utilities.Log;
import net.montoyo.mcef.utilities.Util;

/**
 * A remote resource. Can be downloaded, extracted and checked.
 * @author montoyo
 *
 */
public class Resource {
	
	private String name;
	private String sum;
	
	/**
	 * Constructs a remote resource from its filename and its SHA-1 checksum.
	 * 
	 * @param name The filename of the resource.
	 * @param sum The SHA-1 hash of the file.
	 */
	public Resource(String name, String sum) {
		this.name = name;
		this.sum = sum.trim();
	}
	
	/**
	 * Checks if the file exists. Then check if its checksum is valid.
	 * If the file couldn't be hashed, false will be returned.
	 * 
	 * @return true if (and only if) the file exists and the checksum matches the {@link #sum} field.
	 */
	public boolean exists() {
		File f = new File(ClientProxy.ROOT, name);
		if(!f.exists())
			return false;
		
		String hash = Util.hash(f);
		if(hash == null) {
			Log.warning("Couldn't hash file %s; assuming it doesn't exist.", f.getAbsolutePath());
			return false;
		}
		
		return hash.equalsIgnoreCase(sum);
	}
	
	/**
	 * Downloads the resource from the current mirror.
	 * 
	 * @param ipl Progress listener. May be null.
	 * @param 
	 * @return true if the operation was successful.
	 */
	public boolean download(IProgressListener ipl, String platform) {
		return Util.download(platform + '/' + name, new File(ClientProxy.ROOT, name), ipl);
	}
	
	/**
	 * If the resource is a ZIP archive, it may be extracted using this method.
	 * 
	 * @param ipl Progress listener. May be null.
	 * @return true if the operation was successful.
	 */
	public boolean extract(IProgressListener ipl) {
		Util.secure(ipl).onTaskChanged("Extracting " + name);
		return Util.extract(new File(ClientProxy.ROOT, name), new File(ClientProxy.ROOT));
	}
	
	/**
	 * Gets the filename of this resource.
	 * @return The filename of this resource.
	 */
	public String getFileName() {
		return name;
	}

}
