package net.nerfashton.factory_forcer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForgeConfig;

public class FactoryForcerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue REQUIRE_FOUNDATION;

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
