package com.cinemamod.mcef.mixins;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.Platform;
import net.minecraft.client.resources.ClientPackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

@Mixin(ClientPackSource.class)
public class CefInitMixin {
    private static void setUnixExecutable(File file) {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);

        try {
            Files.setPosixFilePermissions(file.toPath(), perms);
        } catch (IOException e) {
            // Ignore
        }
    }

    private static void setupLibraryPath(Platform platform) throws IOException, URISyntaxException {
        // Check for development environment
        // i.e. mcef-repo/forge/build/cef/<platform>
        File cefPlatformDir = new File("../build/cef/" + platform.getNormalizedName());
        if (cefPlatformDir.exists()) {
            System.setProperty("jcef.path", cefPlatformDir.getCanonicalPath());
            return;
        }

        // Check for .minecraft/mods/mcef-libraries directory, create if not exists
        File mcefLibrariesDir = new File("mods/mcef-libraries");
        if (!mcefLibrariesDir.exists()) {
            mcefLibrariesDir.mkdirs();
        }
        System.setProperty("jcef.path", mcefLibrariesDir.getCanonicalPath());
    }

    @Inject(at = @At("TAIL"), method = "<init>(Ljava/nio/file/Path;)V")
    private void init(CallbackInfo callbackInfo) {
        Platform platform = Platform.getPlatform();

        try {
            setupLibraryPath(platform);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // TODO: Move to org.cef.CefApp
        if (platform.isLinux()) {
            System.loadLibrary("jawt");
        }

        if (platform.isLinux() || platform.isWindows()) {
            if (MCEF.initialize()) {
                System.out.println("Chromium Embedded Framework initialized");
            } else {
                System.out.println("Could not initialize Chromium Embedded Framework");
            }
        }
    }
}
