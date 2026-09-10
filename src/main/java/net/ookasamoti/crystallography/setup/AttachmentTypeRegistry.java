package net.ookasamoti.crystallography.setup;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.entity.TridentVisualData;
import net.minecraft.network.codec.ByteBufCodecs;

public final class AttachmentTypeRegistry {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CrystallographyMod.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TridentVisualData>> TRIDENT_VISUAL =
            ATTACHMENT_TYPES.register("trident_visual", () ->
                    AttachmentType.builder(() -> TridentVisualData.DEFAULT)
                            .sync(TridentVisualData.STREAM_CODEC)
                            .build()
            );

    // ソウルファイア(青い炎)で着火されたかどうかのサーバー側判定結果。クライアントの見た目
    // (炎エフェクトの色)にのみ使うため永続化は不要で、同期のみ行う（詳細は SoulFireIgnitionHooks）。
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> SOUL_FIRE_IGNITED =
            ATTACHMENT_TYPES.register("soul_fire_ignited", () ->
                    AttachmentType.builder(() -> Boolean.FALSE)
                            .sync(ByteBufCodecs.BOOL)
                            .build()
            );

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }

    private AttachmentTypeRegistry() {}
}
