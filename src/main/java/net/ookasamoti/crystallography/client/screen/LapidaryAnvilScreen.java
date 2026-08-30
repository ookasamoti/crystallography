package net.ookasamoti.crystallography.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.menu.LapidaryAnvilMenu;

// LapidaryAnvilScreen.java

public class LapidaryAnvilScreen extends AbstractContainerScreen<LapidaryAnvilMenu> {
    private static final Identifier TEXTURE = Identifier.tryParse(
            CrystallographyMod.MOD_ID + ":textures/gui/lapidary_anvil_gui.png");

    private Button btnCrackOre;
    private Button btnCrackGems;

    public LapidaryAnvilScreen(LapidaryAnvilMenu menu, Inventory inv, Component title) {
        // imageWidth/imageHeight are now final and must be supplied to the super constructor.
        super(menu, inv, title, 176, 198);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;

        this.inventoryLabelY = this.inventoryLabelY + 32;

        btnCrackOre = Button.builder(Component.literal("⛏"), b -> {
            assert Minecraft.getInstance().gameMode != null;
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, LapidaryAnvilMenu.BTN_CRACK_ORE);
        }).bounds(x + 20, y + 80, 20, 20).build();

        int diamondBtnX = x + (116 + 5 * 18 - 20) - 60;
        btnCrackGems = Button.builder(Component.literal("◆"), b -> {
            assert Minecraft.getInstance().gameMode != null;
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, LapidaryAnvilMenu.BTN_CRACK_GEMS);
        }).bounds(diamondBtnX, y + 80, 20, 20).build();

        addRenderableWidget(btnCrackOre);
        addRenderableWidget(btnCrackGems);
        updateButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        btnCrackOre.active  = menu.canCrackOre();
        btnCrackGems.active = menu.canCrackGems();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Dims the world behind the panel (super) then blits our GUI panel texture.
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        assert TEXTURE != null;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos,
                0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }
}
