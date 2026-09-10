package net.ookasamoti.crystallography.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * 特定のエンチャントが付与された本を分解してシジルにする。エンチャント本1冊のみを
 * グリッド内の任意の位置に置くと成立する（クラフト台の shapeless、1枠のみ使用）。
 * どのエンチャントがどのシジルになるかは JSON 側の {@code enchantment}/{@code result} で指定する。
 */
public class SigilFromBookRecipe extends CustomRecipe {
    public static final MapCodec<SigilFromBookRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                ResourceKey.codec(Registries.ENCHANTMENT).fieldOf("enchantment").forGetter(o -> o.enchantment),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(o -> o.result)
            )
            .apply(i, SigilFromBookRecipe::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SigilFromBookRecipe> STREAM_CODEC = StreamCodec.composite(
        ResourceKey.streamCodec(Registries.ENCHANTMENT), o -> o.enchantment,
        ByteBufCodecs.registry(Registries.ITEM), o -> o.result,
        SigilFromBookRecipe::new
    );
    public static final RecipeSerializer<SigilFromBookRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ResourceKey<Enchantment> enchantment;
    private final Item result;

    public SigilFromBookRecipe(ResourceKey<Enchantment> enchantment, Item result) {
        this.enchantment = enchantment;
        this.result = result;
    }

    private ItemStack findBook(CraftingInput input) {
        ItemStack book = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (!book.isEmpty()) return ItemStack.EMPTY; // 2つ目の非空スロットがあれば不成立
            book = s;
        }
        return book;
    }

    private boolean matchesBook(ItemStack book, Level level) {
        if (book.isEmpty() || !book.is(Items.ENCHANTED_BOOK) || book.getCount() != 1) return false;
        Holder<Enchantment> holder;
        try {
            holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
        } catch (RuntimeException e) {
            return false;
        }
        return EnchantmentHelper.getTagEnchantmentLevel(holder, book) > 0;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return matchesBook(findBook(input), level);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return new ItemStack(result);
    }

    @Override
    public RecipeSerializer<SigilFromBookRecipe> getSerializer() {
        return SERIALIZER;
    }
}
