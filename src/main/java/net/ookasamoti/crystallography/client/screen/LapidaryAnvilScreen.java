package net.ookasamoti.crystallography.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.ookasamoti.crystallography.CrystallographyMod;
import org.jetbrains.annotations.NotNull;

public class LapidaryAnvilScreen extends AbstractContainerScreen<LapidaryAnvilMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.tryParse(
            CrystallographyMod.MOD_ID + ":textures/gui/lapidary_anvil_gui.png");

    private Button btnCrackOre;
    private Button btnCrackGems;

    public LapidaryAnvilScreen(LapidaryAnvilMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 180;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;

        btnCrackOre = Button.builder(Component.literal("⛏"), b ->
                {
                    assert Minecraft.getInstance().gameMode != null;
                    Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, LapidaryAnvilMenu.BTN_CRACK_ORE);
                }
        ).bounds(x + 20, y + 90, 20, 20).build();

        btnCrackGems = Button.builder(Component.literal("◆"), b ->
                {
                    assert Minecraft.getInstance().gameMode != null;
                    Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, LapidaryAnvilMenu.BTN_CRACK_GEMS);
                }
        ).bounds(x + 116 + 6*18 - 20, y + 90, 20, 20).build();

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
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
