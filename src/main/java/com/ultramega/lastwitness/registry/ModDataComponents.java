package com.ultramega.lastwitness.registry;

import com.ultramega.lastwitness.data.EchoMarkedData;
import com.ultramega.lastwitness.data.EchoOfPastData;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.lastwitness.LastWitness.MODID;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final Supplier<DataComponentType<EchoOfPastData>> ECHO_OF_PAST = DATA_COMPONENTS.registerComponentType("echo_of_past",
        builder -> builder.persistent(EchoOfPastData.CODEC));
    public static final Supplier<DataComponentType<EchoMarkedData>> ECHO_MARKED = DATA_COMPONENTS.registerComponentType("echo_marked",
        builder -> builder.persistent(EchoMarkedData.CODEC));

    private ModDataComponents() {
    }
}
