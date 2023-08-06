package com.cinemamod.mcef.mixins;

import net.minecraft.client.resources.ClientPackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPackSource.class)
public class CefInitMixin {
    @Inject(at = @At("TAIL"), method = "<init>(Ljava/nio/file/Path;)V")
    private void init(CallbackInfo callbackInfo) {
        System.out.println("CEF INIT MIXIN");
    }
}
