package net.montoyo.mcef.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.montoyo.mcef.client.init.CefInitMenu;
import net.montoyo.mcef.utilities.CefUtil;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MainMenuMixin {

    @Inject(at = @At("HEAD"), method = "setScreen", cancellable = true)
    private void cefInit(Screen p_91153_, CallbackInfo ci) {
        if (p_91153_ instanceof TitleScreen ts) {
            
            if (!CefUtil.isReady()) {
                Minecraft.getInstance().setScreen(new CefInitMenu(ts));
                ci.cancel();
                return;
            }

            CefUtil.runInit();
        }
    }
}