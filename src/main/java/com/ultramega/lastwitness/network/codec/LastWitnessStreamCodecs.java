package com.ultramega.lastwitness.network.codec;

import java.util.function.Function;

import com.mojang.datafixers.util.Function15;
import net.minecraft.network.codec.StreamCodec;

public final class LastWitnessStreamCodecs {
    private LastWitnessStreamCodecs() {
    }

    public static <B, C,
        T1, T2, T3, T4, T5,
        T6, T7, T8, T9, T10,
        T11, T12, T13, T14, T15> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,

        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,

        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,

        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,

        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,

        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,

        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,

        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,

        final StreamCodec<? super B, T9> codec9,
        final Function<C, T9> getter9,

        final StreamCodec<? super B, T10> codec10,
        final Function<C, T10> getter10,

        final StreamCodec<? super B, T11> codec11,
        final Function<C, T11> getter11,

        final StreamCodec<? super B, T12> codec12,
        final Function<C, T12> getter12,

        final StreamCodec<? super B, T13> codec13,
        final Function<C, T13> getter13,

        final StreamCodec<? super B, T14> codec14,
        final Function<C, T14> getter14,

        final StreamCodec<? super B, T15> codec15,
        final Function<C, T15> getter15,

        final Function15<
            T1, T2, T3, T4, T5,
            T6, T7, T8, T9, T10,
            T11, T12, T13, T14, T15,
            C
            > constructor
    ) {
        return StreamCodec.of(
            (buffer, value) -> {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
                codec8.encode(buffer, getter8.apply(value));
                codec9.encode(buffer, getter9.apply(value));
                codec10.encode(buffer, getter10.apply(value));
                codec11.encode(buffer, getter11.apply(value));
                codec12.encode(buffer, getter12.apply(value));
                codec13.encode(buffer, getter13.apply(value));
                codec14.encode(buffer, getter14.apply(value));
                codec15.encode(buffer, getter15.apply(value));
            },
            buffer -> constructor.apply(
                codec1.decode(buffer),
                codec2.decode(buffer),
                codec3.decode(buffer),
                codec4.decode(buffer),
                codec5.decode(buffer),
                codec6.decode(buffer),
                codec7.decode(buffer),
                codec8.decode(buffer),
                codec9.decode(buffer),
                codec10.decode(buffer),
                codec11.decode(buffer),
                codec12.decode(buffer),
                codec13.decode(buffer),
                codec14.decode(buffer),
                codec15.decode(buffer)
            )
        );
    }
}
