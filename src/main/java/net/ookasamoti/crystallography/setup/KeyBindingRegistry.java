package net.ookasamoti.crystallography.setup;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class KeyBindingRegistry {
    public static final String CATEGORY = "key.categories.crystallography";

    public static final KeyMapping KEY_ROD_CYCLE = new KeyMapping(
            "key.crystallography.cycle_rod",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            CATEGORY
    );

    public static final KeyMapping KEY_WAND_CYCLE = new KeyMapping(
            "key.crystallography.cycle_wand",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            CATEGORY
    );

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_ROD_CYCLE);
        event.register(KEY_WAND_CYCLE);
    }
}
