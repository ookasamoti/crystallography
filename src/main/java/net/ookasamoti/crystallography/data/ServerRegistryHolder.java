package net.ookasamoti.crystallography.data;

import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.Nullable;

/**
 * サーバーの {@link net.minecraft.core.RegistryAccess} を静的に保持する。エンチャント等の
 * 動的レジストリはビルトインではないため通常は Level 経由でしか引けないが、
 * {@code CrystalToolLogic#applyComputedStats} のように呼び出し箇所が多く Level を
 * 引数に持たない箇所からも、シジル付与エンチャントの解決のためにレジストリアクセスが要る。
 * サーバー起動〜停止の間だけ有効（シングルプレイ/専用サーバーとも1つのみを保持すれば足りる）。
 */
public final class ServerRegistryHolder {
    private static @Nullable HolderLookup.Provider registries;

    public static void set(@Nullable HolderLookup.Provider provider) { registries = provider; }

    public static @Nullable HolderLookup.Provider get() { return registries; }

    private ServerRegistryHolder() {}
}
