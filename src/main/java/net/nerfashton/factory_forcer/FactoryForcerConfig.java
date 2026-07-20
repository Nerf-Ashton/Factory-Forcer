package net.nerfashton.factory_forcer;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FactoryForcer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FactoryForcerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue REQUIRE_FOUNDATION;

    static {
        BUILDER.push("Placement Rules");

        REQUIRE_FOUNDATION = BUILDER
                .comment("If true, machines cannot be placed on grass, dirt, or sand. (factory_forcer:natural tagged blocks)",
                        "If false, machines only require a roof to be placed.")
                .define("requireFoundation", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
