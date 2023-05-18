package net.montoyo.mcef.mixins;

import net.minecraft.client.resources.AssetIndex;
import net.minecraft.client.resources.ClientPackSource;
import net.montoyo.mcef.utilities.CefUtil;
import net.montoyo.mcef.utilities.download.MCEFDownloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mixin(ClientPackSource.class)
public class CefInitMixin {
	private static void setupLibraryPath() {
		Path minecraftPath = Paths.get("");
		Path modsPath = minecraftPath.resolve("mods");
		Path cinemaModLibrariesPath = modsPath.resolve("cinemamod-libraries");
		
		if (Files.notExists(cinemaModLibrariesPath)) {
			try {
				Files.createDirectory(cinemaModLibrariesPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.setProperty("cinemamod.libraries.path", cinemaModLibrariesPath.toAbsolutePath().toString());
	}
	
	@Inject(at = @At("TAIL"), method = "<init>")
	private void cefInit(File p_118553_, AssetIndex p_118554_, CallbackInfo ci) {
		setupLibraryPath();
		
		Thread td = new Thread(() -> {
			MCEFDownloader.main(new String[]{});
            CefUtil.setReady();
		});
		td.setDaemon(true);
		td.setName("JCEF-Downloader");
		td.start();
	}
}