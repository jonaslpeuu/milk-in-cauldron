package de.whitoutcookies.milk_in_cauldron;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Milk_in_cauldron implements ModInitializer {

    public static final String MOD_ID = "milk_in_cauldron";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Block registry key
    public static final RegistryKey<Block> MILK_CAULDRON_KEY = RegistryKey.of(
            RegistryKeys.BLOCK,
            Identifier.of(MOD_ID, "milk_cauldron"));

    // These will be initialized in onInitialize
    public static CauldronBehavior.CauldronBehaviorMap MILK_CAULDRON_BEHAVIOR;
    public static Block MILK_CAULDRON;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Milk in Cauldron mod!");

        // Create the Milk Cauldron behavior map
        MILK_CAULDRON_BEHAVIOR = CauldronBehavior.createMap("milk");

        // Create the Milk Cauldron block with proper registry key
        MILK_CAULDRON = new LeveledCauldronBlock(
                Biome.Precipitation.NONE,
                MILK_CAULDRON_BEHAVIOR,
                AbstractBlock.Settings.copy(Blocks.CAULDRON).registryKey(MILK_CAULDRON_KEY));

        // Register the milk cauldron block
        Registry.register(Registries.BLOCK, MILK_CAULDRON_KEY, MILK_CAULDRON);

        // Register behaviors
        registerCauldronBehaviors();
    }

    private void registerCauldronBehaviors() {
        // Add behavior: Use milk bucket on empty cauldron -> fill with milk
        CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR.map().put(Items.MILK_BUCKET,
                (state, world, pos, player, hand, stack) -> {
                    return fillCauldronWithMilk(world, pos, player, hand, stack);
                });

        // Add behavior: Use empty bucket on milk cauldron -> get milk bucket
        MILK_CAULDRON_BEHAVIOR.map().put(Items.BUCKET, (state, world, pos, player, hand, stack) -> {
            return emptyCauldron(state, world, pos, player, hand, stack, new ItemStack(Items.MILK_BUCKET));
        });
    }

    private static ActionResult fillCauldronWithMilk(World world, BlockPos pos, PlayerEntity player, Hand hand,
            ItemStack stack) {
        if (!world.isClient()) {
            Item item = stack.getItem();
            player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(Items.BUCKET)));
            player.incrementStat(Stats.FILL_CAULDRON);
            player.incrementStat(Stats.USED.getOrCreateStat(item));
            world.setBlockState(pos, MILK_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3));
            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
        return ActionResult.SUCCESS;
    }

    private static ActionResult emptyCauldron(BlockState state, World world, BlockPos pos, PlayerEntity player,
            Hand hand, ItemStack stack, ItemStack output) {
        // Only empty if cauldron is full (level 3)
        if (state.get(LeveledCauldronBlock.LEVEL) != 3) {
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        }

        if (!world.isClient()) {
            Item item = stack.getItem();
            player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, output));
            player.incrementStat(Stats.USE_CAULDRON);
            player.incrementStat(Stats.USED.getOrCreateStat(item));
            world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
        return ActionResult.SUCCESS;
    }
}
