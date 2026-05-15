package net.ookasamoti.crystallography.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.client.screen.JewelryTableMenu;

public final class ModNet {
    private ModNet() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNet::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        var reg = event.registrar("1");

        CrystallographyMod.LOGGER.info("[Net] Register payloads");

        reg.playToServer(
                JewelryActionC2S.TYPE,
                JewelryActionC2S.STREAM_CODEC,
                (msg, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.containerMenu instanceof JewelryTableMenu menu)) return;
                    if (menu.containerId != msg.containerId()) return;

                    switch (msg.action()) {
                        case JewelryActionC2S.ACTION_SELECT_FORM    -> menu.serverSelectForm(msg.param());
                        case JewelryActionC2S.ACTION_TOGGLE_CRYSTAL -> menu.serverToggleCrystal(msg.param());
                        case JewelryActionC2S.ACTION_REGISTER       -> menu.serverRegister(msg.param());
                    }
                })
        );
    }
}
