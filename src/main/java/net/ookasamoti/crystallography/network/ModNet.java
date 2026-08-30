package net.ookasamoti.crystallography.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.common.menu.JewelryTableMenu;

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
                        case JewelryActionC2S.ACTION_SELECT_FORM      -> menu.serverSelectForm(msg.param());
                        case JewelryActionC2S.ACTION_TOGGLE_CRYSTAL   -> menu.serverToggleCrystal(msg.param());
                        case JewelryActionC2S.ACTION_REGISTER         -> menu.serverRegister(msg.param());
                        case JewelryActionC2S.ACTION_EDIT_REGISTRY    -> menu.serverStartEditRegistry(msg.param());
                        case JewelryActionC2S.ACTION_CANCEL           -> menu.serverCancelEdit();
                        case JewelryActionC2S.ACTION_DELETE_REGISTRY  -> menu.serverDeleteRegistry();
                        case JewelryActionC2S.ACTION_SWAP_REGISTRY    -> menu.serverSwapRegistry(msg.param());
                    }
                })
        );

        reg.playToServer(
                ToolCycleC2S.TYPE,
                ToolCycleC2S.STREAM_CODEC,
                (msg, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;

                    ItemStack stack = sp.getMainHandItem();
                    if (!(stack.getItem() instanceof ToolBase)) {
                        stack = sp.getOffhandItem();
                        if (!(stack.getItem() instanceof ToolBase)) return;
                    }

                    var registered = ToolBase.getLoadout(stack).entries().stream()
                            .mapToInt(ToolLoadout::slotIndex)
                            .sorted()
                            .boxed()
                            .collect(java.util.stream.Collectors.toList());
                    if (registered.isEmpty()) return;

                    int current = ToolBase.getActiveIndex(stack);
                    int pos = registered.indexOf(current);
                    if (pos < 0) pos = 0;
                    int next = Math.floorMod(pos + msg.delta(), registered.size());
                    ToolBase.setActiveIndex(stack, registered.get(next));
                    ToolBase.applyComputedStats(stack);
                })
        );
    }
}
