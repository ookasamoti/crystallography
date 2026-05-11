package net.ookasamoti.crystallography.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.ookasamoti.crystallography.CrystallographyMod; // MOD_ID / LOGGER を想定
import net.ookasamoti.crystallography.client.screen.JewelryTableMenu;

public final class ModNet {
    private ModNet() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNet::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // プロトコルバージョン文字列（NeoForge docs の例でも "1" を使う）
        var reg = event.registrar("1");

        CrystallographyMod.LOGGER.info("[Net] Register payloads");

        reg.playToServer(
                RotateRingC2S.TYPE,
                RotateRingC2S.STREAM_CODEC,
                (msg, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.containerMenu instanceof JewelryTableMenu menu)) return;
                    if (menu.containerId != msg.containerId()) return;

                    if (msg.ring() == RotateRingC2S.RING_CRYSTALS) {
                        menu.rotateCrystalsViewServer(msg.steps());
                    }
                })
        );
    }
}
