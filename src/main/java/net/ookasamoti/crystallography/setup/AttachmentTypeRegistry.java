package net.ookasamoti.crystallography.setup;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.entity.TridentVisualData;

public final class AttachmentTypeRegistry {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CrystallographyMod.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TridentVisualData>> TRIDENT_VISUAL =
            ATTACHMENT_TYPES.register("trident_visual", () ->
                    AttachmentType.builder(() -> TridentVisualData.DEFAULT)
                            .sync(TridentVisualData.STREAM_CODEC)
                            .build()
            );

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }

    private AttachmentTypeRegistry() {}
}
