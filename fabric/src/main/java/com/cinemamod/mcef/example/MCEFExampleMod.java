package com.cinemamod.mcef.example;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class MCEFExampleMod {
    public KeyMapping key = new KeyMapping(
            "Open Browser", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F10, "key.categories.misc"
    );
    Minecraft mc = Minecraft.getInstance();

    public void onTick() {
        // Check if our key was pressed
        if(key.isDown() && !(mc.screen instanceof ExampleScreen)) {
            //Display the web browser UI.
            mc.setScreen(new ExampleScreen(
                    Component.literal("Example Screen")
            ));
        }
    }

    public MCEFExampleMod() {
        ClientTickEvents.START_CLIENT_TICK.register((client) -> onTick());
    }
}
