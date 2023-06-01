package net.montoyo.mcef.utilities.download;

import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.utilities.IProgressListener;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public class ModernDownload {
	protected static final String[] urls = new String[]{
			"cinemamod:https://github.com/CinemaMod/cinemamod-jcefbuild/releases/download/1.0.8/",
			"jcefmvn:https://github.com/jcefmaven/jcefbuild/releases/download/1.0.46/",
	};
	
	public static boolean download(IProgressListener listener) {
		listener.onTaskChanged("1:Downloading from Git");
		
		Path librariesPath = Paths.get(System.getProperty("cinemamod.libraries.path"));
		File info = new File(librariesPath + "/info.txt");
		if (info.exists())
			return true;
		
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
		
		listener.onTaskChanged("2:Download");
		String[] allUrls = new String[urls.length + MCEF.FALLBACK_URLS_GIT.length];
		System.arraycopy(urls, 0, allUrls, 0, urls.length);
		System.arraycopy(MCEF.FALLBACK_URLS_GIT, 0, allUrls, urls.length, MCEF.FALLBACK_URLS_GIT.length);
		for (String urlStr : allUrls) {
			String[] split = urlStr.split(":", 2);
			
			try {
				TarArchiveInputStream tarArchive;
				
				String root = "bin/lib/" + plat;
				
				int count = 0;
				
				listener.onTaskChanged("3:" + split[0]);
				{ // codeblock so the debugger can exist
					URL url = new URL(split[1] + platform + ".tar.gz");
					URLConnection connection = url.openConnection();
					InputStream stream = connection.getInputStream();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					baos.write(stream.readAllBytes());
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
				
				downloaded = true;
				break;
			} catch (Throwable ignored) {
			}
		}
		
		if (downloaded) {
			try {
				FileOutputStream outputStream = new FileOutputStream(info);
				// TODO: actually write data, lol
				outputStream.close();
			} catch (Throwable err) {
				err.printStackTrace();
			}
		}
		
		return downloaded;
	}
}
