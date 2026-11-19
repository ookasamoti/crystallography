package net.ookasamoti.crystallography.setup;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.ookasamoti.crystallography.CrystallographyMod;
import org.lwjgl.glfw.GLFW;

public class KeyBindingRegistry {
    // Key categories are now identified by a ResourceLocation (Identifier) rather
    // than a raw translation-key string; the category must be registered separately.
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "general"));

    public static final KeyMapping KEY_MAINHAND_CYCLE = new KeyMapping(
            "key.crystallography.cycle_mainhand",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            CATEGORY
    );

    public static final KeyMapping KEY_OFFHAND_CYCLE = new KeyMapping(
            "key.crystallography.cycle_offhand",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            CATEGORY
    );

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(KEY_MAINHAND_CYCLE);
        event.register(KEY_OFFHAND_CYCLE);
    }
}
