package net.ookasamoti.crystallography.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.TridentModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.ookasamoti.crystallography.common.item.tool.CrystalColorHelper;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * Same {@link TridentModel} geometry and pipeline as vanilla's {@code TridentSpecialRenderer},
 * but draws it as four layered passes (an untinted tier "frame" plus three crystal-tinted
 * overlays keyed to crystal slots 0/1/2 = center/left/right, mirroring the flat item icon's
 * {@code tints} array) so the held/aiming model reflects the tool's actual crystal colours
 * instead of a single static texture.
 *
 * <p>The overlay layers are mostly-transparent masks, so — like vanilla's banner/shield pattern
 * overlays (see {@code BannerRenderer.submitPatternLayer}) — they are rendered as items-atlas
 * sprites rather than raw standalone texture binds; a direct {@code RenderTypes.entityCutout}/
 * {@code entityTranslucent(Identifier)} bind does not respect the mask's alpha in this special
 * model pipeline (the transparent areas come out solid black), while the sprite-atlas route
 * (the same one flat item-icon layers already use, since these textures are already referenced
 * as icon layers) does.
 */
public class CrystalTridentSpecialRenderer implements SpecialModelRenderer<CrystalTridentSpecialRenderer.TridentColors> {
    private final TridentModel model;
    private final Identifier frameTexture;
    private final SpriteGetter sprites;
    private final SpriteId centerSprite;
    private final SpriteId leftSprite;
    private final SpriteId rightSprite;

    public CrystalTridentSpecialRenderer(
            TridentModel model,
            Identifier frameTexture,
            SpriteGetter sprites,
            SpriteId centerSprite,
            SpriteId leftSprite,
            SpriteId rightSprite) {
        this.model = model;
        this.frameTexture = frameTexture;
        this.sprites = sprites;
        this.centerSprite = centerSprite;
        this.leftSprite = leftSprite;
        this.rightSprite = rightSprite;
    }

    @Override
    public @Nullable TridentColors extractArgument(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        var registryAccess = level != null ? level.registryAccess() : null;
        int center = CrystalColorHelper.colorForSlot(stack, registryAccess, 0);
        int left = CrystalColorHelper.colorForSlot(stack, registryAccess, 1);
        int right = CrystalColorHelper.colorForSlot(stack, registryAccess, 2);
        return new TridentColors(center, left, right);
    }

    @Override
    public void submit(
            @Nullable TridentColors colors,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor) {
        int center = colors != null ? colors.center() : -1;
        int left = colors != null ? colors.left() : -1;
        int right = colors != null ? colors.right() : -1;

        submitNodeCollector.submitModelPart(
            this.model.root(), poseStack, RenderTypes.entitySolid(this.frameTexture),
            lightCoords, overlayCoords, null, false, hasFoil, -1, null, outlineColor
        );
        submitOverlay(submitNodeCollector, poseStack, lightCoords, overlayCoords, outlineColor, this.centerSprite, center);
        submitOverlay(submitNodeCollector, poseStack, lightCoords, overlayCoords, outlineColor, this.leftSprite, left);
        submitOverlay(submitNodeCollector, poseStack, lightCoords, overlayCoords, outlineColor, this.rightSprite, right);
    }

    private void submitOverlay(
            SubmitNodeCollector submitNodeCollector,
            PoseStack poseStack,
            int lightCoords,
            int overlayCoords,
            int outlineColor,
            SpriteId sprite,
            int tint) {
        submitNodeCollector.submitModel(
            this.model,
            Unit.INSTANCE,
            poseStack,
            sprite.renderType(RenderTypes::entityTranslucent),
            lightCoords,
            overlayCoords,
            tint,
            this.sprites.get(sprite),
            outlineColor,
            null
        );
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.model.root().getExtentsForGui(poseStack, output);
    }

    public record TridentColors(int center, int left, int right) {
    }

    public record Unbaked(Identifier frameTexture, Identifier centerTexture, Identifier leftTexture, Identifier rightTexture)
            implements SpecialModelRenderer.Unbaked<TridentColors> {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Identifier.CODEC.fieldOf("frame_texture").forGetter(Unbaked::frameTexture),
                    Identifier.CODEC.fieldOf("center_texture").forGetter(Unbaked::centerTexture),
                    Identifier.CODEC.fieldOf("left_texture").forGetter(Unbaked::leftTexture),
                    Identifier.CODEC.fieldOf("right_texture").forGetter(Unbaked::rightTexture)
                )
                .apply(i, Unbaked::new)
        );

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        public CrystalTridentSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new CrystalTridentSpecialRenderer(
                new TridentModel(context.entityModelSet().bakeLayer(ModelLayers.TRIDENT)),
                this.frameTexture,
                context.sprites(),
                new SpriteId(TextureAtlas.LOCATION_ITEMS, this.centerTexture),
                new SpriteId(TextureAtlas.LOCATION_ITEMS, this.leftTexture),
                new SpriteId(TextureAtlas.LOCATION_ITEMS, this.rightTexture)
            );
        }
    }
}
