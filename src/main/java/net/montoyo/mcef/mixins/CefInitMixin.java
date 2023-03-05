package net.montoyo.mcef.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.main.Main;
import net.montoyo.mcef.client.ClientProxy;
import net.montoyo.mcef.utilities.CefUtil;
import net.montoyo.mcef.utilities.Log;
import net.montoyo.mcef.utilities.MCEFDownloader;
import org.cef.OS;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mixin(Main.class)
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

    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;finishInitialization()V"), method = "m_239872_")
    private static void cefInit(String[] p_239873_, boolean p_239874_, CallbackInfo ci) {
        setupLibraryPath();

        MCEFDownloader.main(new String[]{});

        // TEMP HACK
        if (OS.isLinux()) {
            System.load("/usr/lib/jvm/java-17-openjdk-17.0.3.0.7-1.fc36.x86_64/lib/libjawt.so");
        }

        if (OS.isWindows() || OS.isLinux()) {
            if (CefUtil.init(new ClientProxy())) {
                Log.info("Chromium Embedded Framework initialized");
            } else {
                Log.warning("Could not initialize Chromium Embedded Framework");
            }
        }
    }

}