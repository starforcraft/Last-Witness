package com.ultramega.lastwitness.client;

import com.ultramega.lastwitness.network.GhostReplayPayload;
import com.ultramega.lastwitness.tracking.EntitySnapshot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class GhostReplayClient {
    private static final String GHOST_TAG = "lastwitness_ghost";
    private static final int END_HOLD_TICKS = 20;
    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(-1_000_000_000);
    private static final List<ActiveGhost> ACTIVE_GHOSTS = new ArrayList<>();

    private static ClientLevel activeLevel;

    private GhostReplayClient() {
    }

    public static void handlePayload(final GhostReplayPayload payload, final IPayloadContext context) {
        startReplay(payload);
    }

    public static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null) {
            clearGhosts();
            activeLevel = null;
            return;
        }

        if (activeLevel != level) {
            clearGhosts();
            activeLevel = level;
        }

        final Iterator<ActiveGhost> iterator = ACTIVE_GHOSTS.iterator();
        while (iterator.hasNext()) {
            final ActiveGhost replay = iterator.next();
            if (!replay.tick(level)) {
                replay.remove(level);
                iterator.remove();
            }
        }
    }

    public static void onInteraction(final InputEvent.InteractionKeyMappingTriggered event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.hitResult instanceof EntityHitResult entityHit) || !isGhost(entityHit.getEntity())) {
            return;
        }

        event.setSwingHand(false);
        event.setCanceled(true);
    }

    private static void startReplay(final GhostReplayPayload payload) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null || payload.snapshots().isEmpty()) {
            return;
        }

        final Identifier entityTypeId = Identifier.tryParse(payload.sourceEntityType());
        if (entityTypeId == null) {
            return;
        }

        final Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId);
        if (entityType.isEmpty()) {
            return;
        }

        final Entity created = entityType.get().create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof LivingEntity ghost)) {
            return;
        }

        final int ghostId = NEXT_ENTITY_ID.getAndDecrement();
        final UUID ghostUuid = UUID.randomUUID();
        final List<EntitySnapshot> snapshots = payload.snapshots();
        final Vec3 anchorOffset = calculateAnchorOffset(ghost, payload);
        final List<ReplayFrame> replayFrames = buildReplayFrames(ghost, snapshots, anchorOffset);

        applyInitialFrame(ghost, replayFrames.getFirst(), ghostId, ghostUuid);
        level.addEntity(ghost);
        ACTIVE_GHOSTS.add(new ActiveGhost(ghost, replayFrames, ghostId, ghostUuid));
        activeLevel = level;
    }

    private static Vec3 calculateAnchorOffset(final LivingEntity ghost, final GhostReplayPayload payload) {
        payload.snapshots().getLast().loadInto(ghost);
        return new Vec3(payload.anchorX(), payload.anchorY(), payload.anchorZ())
            .subtract(ghost.position());
    }

    private static List<ReplayFrame> buildReplayFrames(final LivingEntity ghost, final List<EntitySnapshot> snapshots, final Vec3 anchorOffset) {
        final List<ReplayFrame> frames = new ArrayList<>(snapshots.size());
        for (final EntitySnapshot snapshot : snapshots) {
            snapshot.loadInto(ghost);
            frames.add(new ReplayFrame(snapshot, ghost.position().add(anchorOffset)));
        }
        return List.copyOf(frames);
    }

    private static void applyInitialFrame(final LivingEntity ghost, final ReplayFrame frame, final int ghostId, final UUID ghostUuid) {
        frame.snapshot().loadInto(ghost);
        moveToPosition(ghost, frame.position());
        applyGhostState(ghost, ghostId, ghostUuid);
        ghost.setOldPosAndRot(ghost.position(), ghost.getYRot(), ghost.getXRot());
    }

    private static void applySnapshotState(final LivingEntity ghost, final ReplayFrame frame, final int ghostId, final UUID ghostUuid) {
        frame.snapshot().loadInto(ghost);
        applyGhostState(ghost, ghostId, ghostUuid);
    }

    private static void moveToPosition(final LivingEntity ghost, final Vec3 position) {
        ghost.snapTo(position.x(), position.y(), position.z(), ghost.getYRot(), ghost.getXRot());
    }

    private static Vec3 interpolatePosition(final ReplayFrame from,
                                            final ReplayFrame to,
                                            final long fromOffset,
                                            final long toOffset,
                                            final long elapsedTicks) {
        final long duration = Math.max(1L, toOffset - fromOffset);
        final double progress = Math.clamp((double) (elapsedTicks - fromOffset) / duration, 0.0D, 1.0D);
        return from.position().lerp(to.position(), progress);
    }

    private static void applyGhostState(final LivingEntity ghost, final int ghostId, final UUID ghostUuid) {
        ghost.setId(ghostId);
        ghost.setUUID(ghostUuid);
        ghost.addTag(GHOST_TAG);
        ghost.setSilent(true);
        ghost.setInvulnerable(true);
        if (!ghost.isAlive()) {
            ghost.setHealth(1.0F);
        }
        ghost.setInvisible(false);
        ghost.setGlowingTag(true);
        ghost.setCustomNameVisible(false);
        ghost.setNoGravity(true);
        ghost.noPhysics = true;
        ghost.setDeltaMovement(Vec3.ZERO);

        if (ghost instanceof Mob mob) {
            mob.setNoAi(true);
        }
    }

    private static boolean isGhost(final Entity entity) {
        return entity.entityTags().contains(GHOST_TAG);
    }

    private static void clearGhosts() {
        if (activeLevel != null) {
            for (final ActiveGhost ghost : ACTIVE_GHOSTS) {
                ghost.remove(activeLevel);
            }
        }
        ACTIVE_GHOSTS.clear();
    }

    private record ReplayFrame(EntitySnapshot snapshot, Vec3 position) {
    }

    private static final class ActiveGhost {
        private final LivingEntity entity;
        private final List<ReplayFrame> frames;
        private final int ghostId;
        private final UUID ghostUuid;
        private final long firstGameTime;

        private int nextFrame;
        private long elapsedTicks;
        private int endHoldTicks;

        private ActiveGhost(final LivingEntity entity, final List<ReplayFrame> frames, final int ghostId, final UUID ghostUuid) {
            this.entity = entity;
            this.frames = List.copyOf(frames);
            this.ghostId = ghostId;
            this.ghostUuid = ghostUuid;
            this.firstGameTime = this.frames.getFirst().snapshot().gameTime();
            this.nextFrame = 1;
            this.elapsedTicks = 1L;
        }

        private boolean tick(final ClientLevel level) {
            if (this.entity.isRemoved() || this.entity.level() != level) {
                return false;
            }

            final Vec3 previousPosition = this.entity.position();
            final float previousYRot = this.entity.getYRot();
            final float previousXRot = this.entity.getXRot();

            while (this.nextFrame < this.frames.size() && this.frameOffset(this.nextFrame) <= this.elapsedTicks) {
                applySnapshotState(this.entity, this.frames.get(this.nextFrame), this.ghostId, this.ghostUuid);
                this.nextFrame++;
            }

            final ReplayFrame currentFrame = this.frames.get(this.nextFrame - 1);
            final Vec3 targetPosition;
            if (this.nextFrame < this.frames.size()) {
                targetPosition = interpolatePosition(
                    currentFrame,
                    this.frames.get(this.nextFrame),
                    this.frameOffset(this.nextFrame - 1),
                    this.frameOffset(this.nextFrame),
                    this.elapsedTicks
                );
            } else {
                targetPosition = currentFrame.position();
            }

            moveToPosition(this.entity, targetPosition);
            this.entity.setOldPosAndRot(previousPosition, previousYRot, previousXRot);
            applyGhostState(this.entity, this.ghostId, this.ghostUuid);
            this.entity.calculateEntityAnimation(this.entity instanceof FlyingAnimal);

            if (this.nextFrame < this.frames.size()) {
                this.elapsedTicks++;
                return true;
            }

            return this.endHoldTicks++ < END_HOLD_TICKS;
        }

        private long frameOffset(final int index) {
            return Math.max(0L, this.frames.get(index).snapshot().gameTime() - this.firstGameTime);
        }

        private void remove(final ClientLevel level) {
            if (!this.entity.isRemoved()) {
                level.removeEntity(this.entity.getId(), Entity.RemovalReason.DISCARDED);
            }
        }
    }
}
