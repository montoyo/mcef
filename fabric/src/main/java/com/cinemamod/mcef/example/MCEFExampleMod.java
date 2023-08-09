package com.cinemamod.mcef.example;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class MCEFExampleMod {
    private static final Minecraft minecraft = Minecraft.getInstance();

    public static final KeyMapping KEY_MAPPING = new KeyMapping(
            "Open Browser", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F10, "key.categories.misc"
    );

    public MCEFExampleMod() {
        ClientTickEvents.START_CLIENT_TICK.register((client) -> onTick());
    }

    public void onTick() {
        // Check if our key was pressed
        if (KEY_MAPPING.isDown() && !(minecraft.screen instanceof ExampleScreen)) {
            //Display the web browser UI.
            minecraft.setScreen(new ExampleScreen(
                    Component.literal("Example Screen")
            ));
        }
    }
}
