package net.ookasamoti.crystallography.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.ookasamoti.crystallography.setup.AttachmentTypeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * バニラのFire/SoulFireブロックは「着火元がソウルファイアかどうか」を見分ける専用のフックを
 * 公開していない({@code entityInside} 自体を両ブロックが同じ1メソッドとして共有している)。
 * これまではダメージイベントや毎tickのブロックスキャンで間接的に推測していたが、着火元の
 * イベントが来ない瞬間(無敵時間・高速移動によるブロック接触の見落とし等)が残り、炎の色が
 * 一瞬ズレて見えるケースがあった。
 * <p>
 * このmixinは接触の瞬間に実際の{@link BlockState}へ直接アクセスできる{@code entityInside}
 * 自体にフックし、ソウルファイアかどうかをその場で確定させる。引数が{@code Entity}型のため、
 * {@code LivingEntity}(mob・プレイヤー)だけでなく矢({@code AbstractArrow})も同じ経路でカバーする。
 * <p>
 * サーバー限定にしていないのが重要なポイント。{@code checkInsideBlocks}(この呼び出し元)は
 * クライアント側でも実際に走る(足音・パーティクル等のローカル演出のため)ので、ここをクライアント
 * 側でも実行して見た目用の{@link AttachmentTypeRegistry#SOUL_FIRE_IGNITED}をローカルに即時セット
 * する。これにより、バニラ自身の「燃えている」同期フラグ(サーバー→クライアントの別パケットで届く)
 * が届くより前に色の判定だけはクライアント側で先に確定しており、炎が最初に表示される瞬間には
 * 既に青/オレンジの判定が済んでいる。以前はこの判定がサーバー専用の{@code Attachment}同期(バニラの
 * 発火フラグとは別パケット)任せだったため、同期パケットの到着順によっては一瞬だけ通常色の炎が
 * 見えることがあった。攻撃ダメージ倍率などサーバー権威が必要なロジック({@link SoulFireIgnitionHooks}
 * ・{@link net.ookasamoti.crystallography.common.entity.ArrowSoulFireHooks})は引き続きサーバー側の
 * {@code Entity}インスタンス(クライアント側の描画用コピーとは別オブジェクト)の値だけを参照するため、
 * クライアント側でのこのローカル設定がゲームプレイに影響することはない。
 */
@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockMixin {

    @Inject(method = "entityInside", at = @At("HEAD"))
    private void crystallography$onEntityInside(
            BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isFirstTick, CallbackInfo ci) {
        if (entity.fireImmune()) return;
        if (!(entity instanceof LivingEntity) && !(entity instanceof AbstractArrow)) return;

        boolean soulFire = state.is(Blocks.SOUL_FIRE);
        boolean normalFire = state.is(Blocks.FIRE);
        if (!soulFire && !normalFire) return;

        boolean current = Boolean.TRUE.equals(entity.getData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED));
        if (soulFire && !current) {
            entity.setData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED, true);
        } else if (normalFire && current) {
            entity.setData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED, false);
        }
    }
}
