package net.nerfashton.factory_forcer;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(FactoryForcer.MOD_ID)
public class FactoryForcer
{
    public static final String MOD_ID = "factory_forcer";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FactoryForcer(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("Initializing FactoryForcer");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class FactoryForcerEventHandler {
        public static final TagKey<Block> INDOOR_ONLY = BlockTags.create(new ResourceLocation(MOD_ID, "indoor_only"));

        @SubscribeEvent
        public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
            if (!(event.getEntity() instanceof Player player)) {
                return;
            }

            Level level = player.level();
            if (level.isClientSide()) {
                return;
            }

            BlockPos pos = event.getPos();

            if (event.getPlacedBlock().is(INDOOR_ONLY)) {
                int roofHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
                if (pos.getY() >= roofHeight - 1) {
                    // Cancel the event, stopping the block from being placed.
                    event.setCanceled(true);

                    // Play a fail sound and show a message in the action bar (true = action bar, false = chat)
                    level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.translatable("message.factory_forcer.warning"), true);
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
                return; // Abort the event early. The actual roof is still intact above.
            }

            for (int y = brokenPos.getY() - 1; y >= level.getMinBuildHeight(); y--) {
                BlockPos checkPos = new BlockPos(brokenPos.getX(), y, brokenPos.getZ());
                BlockState state = level.getBlockState(checkPos);

                if (state.isAir()) {
                    continue;
                }

                if (state.is(INDOOR_ONLY)) {
                    level.destroyBlock(checkPos, true);
                    System.out.println("block broken at: " + checkPos.getX() + " " + checkPos.getY() + " " + checkPos.getZ());

                    continue;
                }

                if (state.blocksMotion()) {
                    break;
                }
            }
        }
    }
}
