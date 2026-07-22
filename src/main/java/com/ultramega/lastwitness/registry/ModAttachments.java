package com.ultramega.lastwitness.registry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import static com.ultramega.lastwitness.LastWitness.MODID;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    public static final Supplier<AttachmentType<Boolean>> CARRIES_ECHO = ATTACHMENT_TYPES.register(
        "carries_echo",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("value"))
            .build()
    );

    private ModAttachments() {
    }
}
