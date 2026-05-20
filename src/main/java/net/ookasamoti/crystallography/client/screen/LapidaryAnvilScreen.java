package net.ookasamoti.crystallography.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.menu.LapidaryAnvilMenu;
import org.jetbrains.annotations.NotNull;

// LapidaryAnvilScreen.java

public class LapidaryAnvilScreen extends AbstractContainerScreen<LapidaryAnvilMenu> {
    private static final Identifier TEXTURE = Identifier.tryParse(
            CrystallographyMod.MOD_ID + ":textures/gui/lapidary_anvil_gui.png");

    private Button btnCrackOre;
    private Button btnCrackGems;

    public LapidaryAnvilScreen(LapidaryAnvilMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
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
    protected void renderBg(GuiGraphicsExtractor guiGraphics, float partialTick, int mouseX, int mouseY) {
        assert TEXTURE != null;
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}

