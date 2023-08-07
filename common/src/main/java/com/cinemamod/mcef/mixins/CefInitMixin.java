package com.cinemamod.mcef.mixins;

import com.cinemamod.mcef.CefUtil;
import com.cinemamod.mcef.Platform;
import net.minecraft.client.resources.ClientPackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Scanner;
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

        //
        // CEF library extraction
        //
        URL cefManifestURL = CefInitMixin.class.getClassLoader().getResource("cef/manifest.txt");

        if (cefManifestURL == null) {
            return;
        }

        try (InputStream cefManifestInputStream = cefManifestURL.openStream();
             Scanner scanner = new Scanner(cefManifestInputStream)) {
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                String fileHash = line.split("  ")[0]; // TODO: check hash
                String relFilePath = line.split("  ")[1];
                URL cefResourceURL = CefInitMixin.class.getClassLoader().getResource("cef/" + relFilePath);

                if (cefResourceURL == null) {
                    continue;
                }

                try (InputStream cefResourceInputStream = cefResourceURL.openStream()) {
                    File cefResourceFile = new File(mcefLibrariesDir, relFilePath);

                    if (cefResourceFile.exists()) {
                        continue;
                    }

                    cefResourceFile.getParentFile().mkdirs(); // For when we run across a nested file, i.e. locales/sl.pak
                    Files.copy(cefResourceInputStream, cefResourceFile.toPath());
                    if (platform.isLinux()) {
                        if (cefResourceFile.getName().contains("chrome-sandbox")
                                || cefResourceFile.getName().contains("jcef_helper")) {
                            setUnixExecutable(cefResourceFile);
                        }
                    }
                }
            }
        }
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
            if (CefUtil.init()) {
                System.out.println("Chromium Embedded Framework initialized");
            } else {
                System.out.println("Could not initialize Chromium Embedded Framework");
            }
        }
    }
}
