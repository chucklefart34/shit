package com.dropzone.loot;

import com.dropzone.DZConfig;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/** Adds an item to a loot table with a chance and a count range. Toggled by the chestLoot config. */
public class AddItemModifier extends LootModifier {
    public static final Supplier<Codec<AddItemModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst)
                    .and(ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item))
                    .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
                    .and(Codec.INT.optionalFieldOf("min", 1).forGetter(m -> m.min))
                    .and(Codec.INT.optionalFieldOf("max", 1).forGetter(m -> m.max))
                    .apply(inst, AddItemModifier::new)));

    private final Item item;
    private final float chance;
    private final int min;
    private final int max;

    public AddItemModifier(LootItemCondition[] conditions, Item item, float chance, int min, int max) {
        super(conditions);
        this.item = item;
        this.chance = chance;
        this.min = Math.max(1, min);
        this.max = Math.max(this.min, max);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!DZConfig.CHEST_LOOT.get()) return generatedLoot;
        if (context.getRandom().nextFloat() < chance) {
            int count = min + context.getRandom().nextInt(max - min + 1);
            generatedLoot.add(new ItemStack(item, count));
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
