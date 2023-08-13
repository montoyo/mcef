package com.cinemamod.mcef.mixins;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFDownloader;
import com.cinemamod.mcef.MCEFPlatform;
import net.minecraft.client.resources.ClientPackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
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

    private static void setupLibraryPath() throws IOException {
        final File mcefLibrariesDir;

        // Check for development environment
        // TODO: handle eclipse/others
        // i.e. mcef-repo/forge/build
        File buildDir = new File("../build");
        if (buildDir.exists() && buildDir.isDirectory()) {
            mcefLibrariesDir = new File(buildDir, "mcef-libraries/");
        } else {
            mcefLibrariesDir = new File("mods/mcef-libraries/");
        }

        mcefLibrariesDir.mkdirs();

        System.setProperty("mcef.libraries.path", mcefLibrariesDir.getCanonicalPath());
        System.setProperty("jcef.path", new File(mcefLibrariesDir, MCEFPlatform.getPlatform().getNormalizedName()).getCanonicalPath());
    }

    @Inject(at = @At("TAIL"), method = "<init>(Ljava/nio/file/Path;)V")
    private void init(CallbackInfo callbackInfo) {
        try {
            setupLibraryPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String javaCefCommit = null;

        try {
            javaCefCommit = MCEF.getJavaCefCommit();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("java-cef commit: " + javaCefCommit);

        MCEFDownloader downloader = new MCEFDownloader(javaCefCommit, MCEFPlatform.getPlatform());

        boolean downloadJcefBuild = false;

        // We always download the checksum for the java-cef build
        // We will compare this with mcef-libraries/<platform>.tar.gz.sha256
        // If the contents of the files differ (or it doesn't exist locally), we know we need to redownload JCEF
        try {
            downloadJcefBuild = !downloader.downloadJavaCefChecksum();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (downloadJcefBuild) {
            try {
                downloader.downloadJavaCefBuild(percentComplete -> {});
            } catch (IOException e) {
                e.printStackTrace();
            }

            downloader.extractJavaCefBuild(true, System.out::println);
        }

        MCEFPlatform platform = MCEFPlatform.getPlatform();

        if (platform.isLinux()) {
            File jcefHelperFile = new File(System.getProperty("mcef.libraries.path"), platform.getNormalizedName() + "/jcef_helper");
            setUnixExecutable(jcefHelperFile);
        } else if (platform.isMacOS()) {
            File mcefLibrariesPath = new File(System.getProperty("mcef.libraries.path"));
            File jcefHelperFile = new File(mcefLibrariesPath, platform.getNormalizedName() + "/jcef_app.app/Contents/Frameworks/jcef Helper.app/Contents/MacOS/jcef Helper");
            File jcefHelperGPUFile = new File(mcefLibrariesPath, platform.getNormalizedName() + "/jcef_app.app/Contents/Frameworks/jcef Helper (GPU).app/Contents/MacOS/jcef Helper (GPU)");
            File jcefHelperPluginFile = new File(mcefLibrariesPath, platform.getNormalizedName() + "/jcef_app.app/Contents/Frameworks/jcef Helper (Plugin).app/Contents/MacOS/jcef Helper (Plugin)");
            File jcefHelperRendererFile = new File(mcefLibrariesPath, platform.getNormalizedName() + "/jcef_app.app/Contents/Frameworks/jcef Helper (Renderer).app/Contents/MacOS/jcef Helper (Renderer)");

            setUnixExecutable(jcefHelperFile);
            setUnixExecutable(jcefHelperGPUFile);
            setUnixExecutable(jcefHelperPluginFile);
            setUnixExecutable(jcefHelperRendererFile);
        }

        if (MCEF.initialize()) {
            System.out.println("Chromium Embedded Framework initialized");
        } else {
            System.out.println("Could not initialize Chromium Embedded Framework");
        }
    }
}
