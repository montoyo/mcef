package net.montoyo.mcef.mixins;

import net.minecraft.client.Minecraft;
import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.client.ClientProxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(at = @At("HEAD"), method = "runTick")
	public void preFrame(boolean p_91384_, CallbackInfo ci) {
		if (MCEF.HIGH_FPS)
			((ClientProxy) MCEF.PROXY).onFrame();
	}
}
