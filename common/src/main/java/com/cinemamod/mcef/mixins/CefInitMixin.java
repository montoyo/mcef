package com.cinemamod.mcef.mixins;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFClient;
import net.minecraft.client.resources.ClientPackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;

@Mixin(ClientPackSource.class)
public class CefInitMixin {
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
    }

    @Inject(at = @At("TAIL"), method = "<init>(Ljava/nio/file/Path;)V")
    private void init(CallbackInfo callbackInfo) {
        try {
            setupLibraryPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MCEFClient client = MCEF.getClient();

        if (client != null) {
            System.out.println("Chromium Embedded Framework initialized");
        } else {
            System.out.println("Could not initialize Chromium Embedded Framework");
        }
    }
}
