package net.montoyo.mcef.utilities.download;

import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.utilities.IProgressListener;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public class ModernDownload {
	protected static final String[] urls = new String[]{
			"cinemamod:https://github.com/CinemaMod/cinemamod-jcefbuild/releases/download/1.0.8/",
			"jcefmvn:https://github.com/jcefmaven/jcefbuild/releases/download/1.0.46/",
	};
	
	static double toMB(int src) {
		return ((int) (src * 1e-5)) / 10d;
	}
	
	public static boolean download(IProgressListener listener) {
		listener.onTaskChanged("1:Downloading from Git");
		
		Path librariesPath = Paths.get(System.getProperty("cinemamod.libraries.path"));
		File info = new File(librariesPath + "/info.txt");
		String existing = "";
		if (info.exists()) {
			try {
				InputStream fis = new FileInputStream(info);
				existing = new String(fis.readAllBytes());
				fis.close();
			} catch (Throwable err) {
				err.printStackTrace();
			}
		}
		
		String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		
		String platform;
		if (os.contains("win")) platform = "windows";
		else if (os.contains("mac")) platform = "macosx";
		else if (os.contains("linux")) platform = "linux";
		else platform = "unknown";
		platform += "-" + System.getProperty("os.arch");
		
		// TODO: improve?
		String post = System.getProperty("os.arch").contains("64") ? "64" : "32";
		String plat;
		if (os.contains("win")) plat = "win" + post;
		else if (os.contains("mac")) plat = "mac" + post;
		else if (os.contains("linux")) plat = "linux" + post;
		else plat = "unknown";
		
		boolean downloaded = false;
		
		listener.onTaskChanged("2:Downloading");
		String[] allUrls = new String[urls.length + MCEF.FALLBACK_URLS_GIT.length];
		allUrls[0] = urls[0];
		System.arraycopy(MCEF.FALLBACK_URLS_GIT, 0, allUrls, 1, MCEF.FALLBACK_URLS_GIT.length);
		allUrls[allUrls.length - 1] = urls[1];
		
		String urlSelected = null;
		
		for (String urlStr : allUrls) {
			// if the highest priority url is the currently installed version, then don't download anything
			if (existing.equals(urlStr))
				return true;
			
			String[] split = urlStr.split(":", 2);
			
			try {
				TarArchiveInputStream tarArchive;
				
				String root = "bin/lib/" + plat;
				
				int count = 0;
				
				listener.onTaskChanged("3:" + split[0]);
				{ // codeblock so the debugger can exist
					URL url = new URL(split[1] + platform + ".tar.gz");
					URLConnection connection = url.openConnection();
					int len = connection.getContentLength();
					InputStream stream = connection.getInputStream();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					
					byte[] buf = new byte[16384];
					int incr;
					int progress = 0;
					while ((incr = stream.read(buf)) != -1) {
						progress += incr;
						listener.onTaskChanged("3:" + split[0] + " (" + toMB(progress) + "MB of " + toMB(len) + "MB)");
						baos.write(buf, 0, incr);
						listener.onProgressed(progress / (double) len);
					}
					stream.close();
					
					tarArchive =
							new TarArchiveInputStream(new GZIPInputStream(
									new ByteArrayInputStream(baos.toByteArray())
							));
					while (tarArchive.getNextTarEntry() != null) {
						TarArchiveEntry entry = tarArchive.getCurrentEntry();
						if (entry.isDirectory()) continue;
						if (entry.getName().startsWith(root))
							count += 1;
					}
					tarArchive.close();
					
					tarArchive =
							new TarArchiveInputStream(new GZIPInputStream(
									new ByteArrayInputStream(baos.toByteArray())
							));
				}
				
				listener.onTaskChanged("2:Extract from " + split[0]);
				int i = 0;
				while (tarArchive.getNextTarEntry() != null) {
					TarArchiveEntry entry = tarArchive.getCurrentEntry();
					listener.onTaskChanged("3:" + entry.getName());
					if (entry.getName().startsWith(root)) {
						if (entry.isDirectory()) {
							File localFile = new File(librariesPath + "/" + entry.getName().substring(root.length()));
							localFile.mkdirs();
							continue;
						}
						
						byte[] data = tarArchive.readAllBytes();
						File localFile = new File(librariesPath + "/" + entry.getName().substring(root.length()));
						
						listener.onProgressed(i / (double) count);
						i++;
						
						FileOutputStream outputStream = new FileOutputStream(localFile);
						outputStream.write(data);
						outputStream.flush();
						outputStream.close();
					}
				}
				
				urlSelected = urlStr;
				downloaded = true;
				break;
			} catch (Throwable ignored) {
			}
		}
		
		if (downloaded) {
			try {
				FileOutputStream outputStream = new FileOutputStream(info);
				outputStream.write(urlSelected.getBytes(StandardCharsets.UTF_8));
				outputStream.close();
			} catch (Throwable err) {
				err.printStackTrace();
			}
		}
		
		return downloaded;
	}
}
