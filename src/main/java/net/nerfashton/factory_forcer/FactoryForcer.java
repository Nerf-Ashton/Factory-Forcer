package net.nerfashton.factory_forcer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FactoryForcer.MOD_ID)
public class FactoryForcer {
    public static final String MOD_ID = "factory_forcer";

    public FactoryForcer(FMLJavaModLoadingContext context)
    {
        context.registerConfig(ModConfig.Type.COMMON, FactoryForcerConfig.SPEC);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class FactoryForcerEventHandler {
        public static final TagKey<Block> INDOOR_ONLY = BlockTags.create(ResourceLocation.fromNamespaceAndPath(MOD_ID, "indoor_only"));
        public static final TagKey<Block> NATURE_BLOCKS = BlockTags.create(ResourceLocation.fromNamespaceAndPath(MOD_ID, "natural"));

        @SubscribeEvent
        public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
            if (!(event.getEntity() instanceof Player player)) {
                return;
            }

            BlockPos pos = event.getPos();

            Level level = player.level();
            if (level.isClientSide()) {
                return;
            }

            ServerPlayer serverPlayer = (ServerPlayer) player;
            if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                return;
            }

            if (event.getPlacedBlock().is(INDOOR_ONLY)) {
                BlockPos belowPos = pos.below();
                BlockState belowState = level.getBlockState(belowPos);

                if (belowState.is(NATURE_BLOCKS) && FactoryForcerConfig.REQUIRE_FOUNDATION.get()) {
                    cancelPlacement(event, player, pos, level, "message.factory_forcer.foundation_warning");
                    return;
                }

                int roofHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
                if (pos.getY() >= roofHeight - 1) {
                    cancelPlacement(event, player, pos, level, "message.factory_forcer.outdoor_warning");
                }
            }
        }

        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
                return;
            }

            BlockPos brokenPos = event.getPos();

            int roofHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, brokenPos.getX(), brokenPos.getZ());

            if (brokenPos.getY() < roofHeight - 1) {
                return;
            }

            for (int y = brokenPos.getY() - 1; y >= level.getMinBuildHeight(); y--) {
                BlockPos checkPos = new BlockPos(brokenPos.getX(), y, brokenPos.getZ());
                BlockState state = level.getBlockState(checkPos);

                if (state.isAir()) {
                    continue;
                }

                if (state.is(INDOOR_ONLY)) {
                    level.destroyBlock(checkPos, true);
                    System.out.println("Factory Forcer: block broken at: " + checkPos.getX() + " " + checkPos.getY() + " " + checkPos.getZ());

                    continue;
                }

                if (state.blocksMotion()) {
                    break;
                }
            }
        }
    }

    private static void cancelPlacement(BlockEvent.EntityPlaceEvent event, Player player, BlockPos pos, Level level, String message) {
        event.setCanceled(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().copy());
        player.setItemInHand(InteractionHand.OFF_HAND, player.getOffhandItem().copy());
        level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable(message), true);
    }
}
