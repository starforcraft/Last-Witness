package com.ultramega.lastwitness.client;

import com.ultramega.lastwitness.data.OutsideEntityEvent;
import com.ultramega.lastwitness.data.OutsideEntityReplay;
import com.ultramega.lastwitness.mixin.client.ItemInHandRendererAccessor;
import com.ultramega.lastwitness.network.ReplayPayload;
import com.ultramega.lastwitness.tracking.EntityReplayEvent;
import com.ultramega.lastwitness.tracking.EntitySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.ultramega.lastwitness.LastWitness.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class GhostReplayClient { //TODO: when in first person mode hide any entity/player and only show them for 2 ticks every seconds
    private static final String GHOST_TAG = "lastwitness_ghost";
    private static final int END_HOLD_TICKS = 10;
    private static final float REPLAY_FOG_NEAR = 0.25F;
    private static final float REPLAY_FOG_FAR = 5.0F;
    private static final float CAMERA_YAW_WOBBLE = 0.45F;
    private static final float CAMERA_PITCH_WOBBLE = 0.35F;
    private static final float CAMERA_ROLL_WOBBLE = 1.65F;
    private static final float FOV_WOBBLE = 2.5F;
    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(-1_000_000_000);
    private static final List<ActiveReplay> ACTIVE_GHOSTS = new ArrayList<>();

    @Nullable
    private static ActiveReplay activeFirstPerson;
    @Nullable
    private static EntitySnapshot localPlayerBeforeHud;
    @Nullable
    private static FirstPersonHandState firstPersonHandStateBeforeFrame;
    @Nullable
    private static ClientLevel activeLevel;

    private GhostReplayClient() {
    }

    public static void handlePayload(final ReplayPayload payload, final IPayloadContext context) {
        startReplay(payload.sourceEntity().uuid(), payload.sourceEntity().type(), payload.snapshots(), payload.entityEvents(), payload.firstPerson());
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        restoreHudState();
        restoreFirstPersonHandState();
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            clearReplays();
            activeLevel = null;
            return;
        }

        if (activeLevel != level) {
            clearReplays();
            activeLevel = level;
        }

        final Iterator<ActiveReplay> iterator = ACTIVE_GHOSTS.iterator();
        while (iterator.hasNext()) {
            final ActiveReplay replay = iterator.next();
            if (!replay.tick(level)) {
                replay.remove(level);
                iterator.remove();
            }
        }

        if (activeFirstPerson != null && !activeFirstPerson.tick(level)) {
            activeFirstPerson.remove(level);
            activeFirstPerson = null;
        }
    }

    @SubscribeEvent
    public static void onInteraction(final InputEvent.InteractionKeyMappingTriggered event) {
        if (activeFirstPerson != null) {
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }

        if (!(Minecraft.getInstance().hitResult instanceof EntityHitResult entityHit) || !isGhost(entityHit.getEntity())) {
            return;
        }

        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderFog(final ViewportEvent.RenderFog event) {
        if (activeFirstPerson == null) {
            return;
        }

        final FogData fogData = event.getFogData();
        event.setNearPlaneDistance(REPLAY_FOG_NEAR);
        event.setFarPlaneDistance(REPLAY_FOG_FAR);
        fogData.renderDistanceStart = REPLAY_FOG_NEAR;
        fogData.renderDistanceEnd = REPLAY_FOG_FAR;
        fogData.skyEnd = REPLAY_FOG_FAR;
        fogData.cloudEnd = REPLAY_FOG_FAR;
    }

    @SubscribeEvent
    public static void onComputeFogColor(final ViewportEvent.ComputeFogColor event) {
        if (activeFirstPerson == null) {
            return;
        }

        event.setRed(event.getRed() * 0.18F);
        event.setGreen(event.getGreen() * 0.20F);
        event.setBlue(event.getBlue() * 0.24F);
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(final ViewportEvent.ComputeCameraAngles event) {
        final ActiveReplay replay = activeFirstPerson;
        if (replay == null) {
            return;
        }

        final double time = replay.visualTime(event.getPartialTick());
        event.setYaw(event.getYaw() + (float) Math.sin(time * 0.17D) * CAMERA_YAW_WOBBLE);
        event.setPitch(event.getPitch() + (float) Math.cos(time * 0.21D) * CAMERA_PITCH_WOBBLE);
        event.setRoll(event.getRoll() + (float) Math.sin(time * 0.32D) * CAMERA_ROLL_WOBBLE);
    }

    @SubscribeEvent
    public static void onComputeFov(final ViewportEvent.ComputeFov event) {
        final ActiveReplay replay = activeFirstPerson;
        if (replay == null) {
            return;
        }

        final double time = replay.visualTime(event.getPartialTick());
        event.setFOV(event.getFOV() + (float) Math.sin(time * 0.28D) * FOV_WOBBLE);
    }

    @SubscribeEvent
    public static void onRenderFramePre(final RenderFrameEvent.Pre event) {
        restoreFirstPersonHandState();
        final Minecraft minecraft = Minecraft.getInstance();
        if (activeFirstPerson == null || minecraft.player == null) {
            return;
        }

        final Player player = minecraft.player;
        final LivingEntity replayEntity = activeFirstPerson.entity();
        final ItemInHandRendererAccessor handRenderer = (ItemInHandRendererAccessor) minecraft.getEntityRenderDispatcher().getItemInHandRenderer();

        firstPersonHandStateBeforeFrame = new FirstPersonHandState(
            player.getMainHandItem(),
            player.getOffhandItem(),
            handRenderer.lastWitness$getMainHandItem(),
            handRenderer.lastWitness$getOffHandItem()
        );

        final ItemStack replayMainHand = replayEntity.getMainHandItem().copy();
        final ItemStack replayOffHand = replayEntity.getOffhandItem().copy();

        player.setItemInHand(InteractionHand.MAIN_HAND, replayMainHand);
        player.setItemInHand(InteractionHand.OFF_HAND, replayOffHand);
        handRenderer.lastWitness$setMainHandItem(replayMainHand);
        handRenderer.lastWitness$setOffHandItem(replayOffHand);
    }

    @SubscribeEvent
    public static void onRenderGuiPre(final RenderGuiEvent.Pre event) {
        restoreHudState();
        final Minecraft minecraft = Minecraft.getInstance();
        if (activeFirstPerson == null
            || minecraft.player == null
            || !(activeFirstPerson.entity() instanceof Avatar)) {
            return;
        }

        localPlayerBeforeHud = EntitySnapshot.capture(minecraft.player);
        activeFirstPerson.hudSnapshot().loadInto(minecraft.player);
    }

    @SubscribeEvent
    public static void onRenderGuiPost(final RenderGuiEvent.Post event) {
        restoreHudState();
    }

    @SubscribeEvent
    public static void onRenderFramePost(final RenderFrameEvent.Post event) {
        restoreHudState();
        restoreFirstPersonHandState();
    }

    private static void restoreHudState() {
        if (localPlayerBeforeHud == null) {
            return;
        }

        final EntitySnapshot snapshot = localPlayerBeforeHud;
        localPlayerBeforeHud = null;
        final Player player = Minecraft.getInstance().player;
        if (player != null) {
            snapshot.loadInto(player);
        }
    }

    private static void restoreFirstPersonHandState() {
        if (firstPersonHandStateBeforeFrame == null) {
            return;
        }

        final FirstPersonHandState state = firstPersonHandStateBeforeFrame;
        firstPersonHandStateBeforeFrame = null;

        final Minecraft minecraft = Minecraft.getInstance();
        final Player player = minecraft.player;
        if (player != null) {
            player.setItemInHand(InteractionHand.MAIN_HAND, state.playerMainHand());
            player.setItemInHand(InteractionHand.OFF_HAND, state.playerOffHand());
        }

        final ItemInHandRendererAccessor handRenderer = (ItemInHandRendererAccessor) minecraft.getEntityRenderDispatcher().getItemInHandRenderer();
        handRenderer.lastWitness$setMainHandItem(state.rendererMainHand());
        handRenderer.lastWitness$setOffHandItem(state.rendererOffHand());
    }

    private static void startReplay(final UUID sourceEntityId,
                                    final String sourceEntityType,
                                    final List<EntitySnapshot> snapshots,
                                    final List<EntityReplayEvent> entityEvents,
                                    final boolean firstPerson) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null || snapshots.isEmpty()) {
            return;
        }

        final Identifier entityTypeId = Identifier.tryParse(sourceEntityType);
        if (entityTypeId == null) {
            return;
        }

        final Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId);
        if (entityType.isEmpty()) {
            return;
        }

        final LivingEntity replayEntity = createReplayEntity(level, entityType.get(), sourceEntityId);
        if (replayEntity == null) {
            return;
        }

        if (firstPerson && activeFirstPerson != null) {
            activeFirstPerson.remove(level);
            activeFirstPerson = null;
        }

        final int replayId = NEXT_ENTITY_ID.getAndDecrement();
        final UUID replayUuid = UUID.randomUUID();
        final List<ReplayFrame> replayFrames = buildReplayFrames(replayEntity, snapshots);
        applyInitialFrame(replayEntity, replayFrames.getFirst(), replayId, replayUuid, !firstPerson);

        final ActiveReplay replay = new ActiveReplay(
            replayEntity,
            replayFrames,
            entityEvents,
            replayId,
            replayUuid,
            !firstPerson,
            firstPerson ? minecraft.getCameraEntity() : null,
            firstPerson ? minecraft.options.getCameraType() : null
        );
        level.addEntity(replayEntity);
        replay.tickOutsideEntities(level, 0L);
        replay.replayEventsThrough(0L);
        if (firstPerson) {
            activeFirstPerson = replay;
            replay.enforceCamera();
        } else {
            ACTIVE_GHOSTS.add(replay);
        }
        activeLevel = level;
    }

    private static LivingEntity createReplayEntity(final ClientLevel level, final EntityType<?> entityType, final UUID sourceEntityId) {
        if (entityType == EntityType.PLAYER) {
            return new ReplayMannequin(level, sourceEntityId);
        }

        final Entity created = entityType.create(level, EntitySpawnReason.COMMAND);
        return created instanceof LivingEntity living ? living : null;
    }

    private static List<ReplayFrame> buildReplayFrames(final LivingEntity replayEntity, final List<EntitySnapshot> snapshots) {
        final List<ReplayFrame> frames = new ArrayList<>(snapshots.size());
        for (final EntitySnapshot snapshot : snapshots) {
            snapshot.loadInto(replayEntity);
            frames.add(new ReplayFrame(
                snapshot,
                replayEntity.position(),
                replayEntity.getYRot(),
                replayEntity.getXRot(),
                replayEntity.yBodyRot,
                replayEntity.yHeadRot
            ));
        }
        return List.copyOf(frames);
    }

    private static void applyInitialFrame(final LivingEntity replayEntity,
                                          final ReplayFrame frame,
                                          final int replayId,
                                          final UUID replayUuid,
                                          final boolean externalGhost) {
        frame.snapshot().loadInto(replayEntity);
        applyFrameTransform(replayEntity, frame);
        applyReplayState(replayEntity, replayId, replayUuid, externalGhost);
        replayEntity.setOldPosAndRot(replayEntity.position(), replayEntity.getYRot(), replayEntity.getXRot());
        replayEntity.yBodyRotO = replayEntity.yBodyRot;
        replayEntity.yHeadRotO = replayEntity.yHeadRot;
    }

    private static void applySnapshotState(final LivingEntity replayEntity,
                                           final ReplayFrame frame,
                                           final int replayId,
                                           final UUID replayUuid,
                                           final boolean externalGhost) {
        frame.snapshot().loadInto(replayEntity);
        applyReplayState(replayEntity, replayId, replayUuid, externalGhost);
    }

    private static void applyFrameTransform(final LivingEntity replayEntity, final ReplayFrame frame) {
        final Vec3 position = frame.position();
        replayEntity.snapTo(position.x(), position.y(), position.z(), frame.yRot(), frame.xRot());
        replayEntity.setYBodyRot(frame.yBodyRot());
        replayEntity.setYHeadRot(frame.yHeadRot());
    }

    private static void applyInterpolatedFrameTransform(final LivingEntity replayEntity,
                                                        final ReplayFrame from,
                                                        final ReplayFrame to,
                                                        final long fromOffset,
                                                        final long toOffset,
                                                        final long elapsedTicks) {
        final long duration = Math.max(1L, toOffset - fromOffset);
        final float progress = (float) Math.clamp((double) (elapsedTicks - fromOffset) / duration, 0.0D, 1.0D);
        final Vec3 position = from.position().lerp(to.position(), progress);

        final float yRot = Mth.rotLerp(progress, from.yRot(), to.yRot());
        final float xRot = Mth.lerp(progress, from.xRot(), to.xRot());
        final float yBodyRot = Mth.rotLerp(progress, from.yBodyRot(), to.yBodyRot());
        final float yHeadRot = Mth.rotLerp(progress, from.yHeadRot(), to.yHeadRot());

        replayEntity.snapTo(position.x(), position.y(), position.z(), yRot, xRot);
        replayEntity.setYBodyRot(yBodyRot);
        replayEntity.setYHeadRot(yHeadRot);
    }

    private static void applyReplayState(final LivingEntity replayEntity, final int replayId, final UUID replayUuid, final boolean externalGhost) {
        replayEntity.setId(replayId);
        replayEntity.setUUID(replayUuid);
        if (externalGhost) {
            replayEntity.addTag(GHOST_TAG);
        } else {
            replayEntity.removeTag(GHOST_TAG);
        }
        replayEntity.setSilent(true);
        replayEntity.setInvulnerable(true);
        if (!replayEntity.isAlive()) {
            replayEntity.setHealth(1.0F);
        }
        replayEntity.setInvisible(false);
        replayEntity.setGlowingTag(externalGhost); //TODO: glowing doesn't work
        replayEntity.setCustomNameVisible(false);
        replayEntity.setNoGravity(true);
        replayEntity.noPhysics = true;
        replayEntity.setDeltaMovement(Vec3.ZERO);
        if (replayEntity instanceof Mob mob) {
            mob.setNoAi(true);
        }
    }

    private static boolean isGhost(final Entity entity) {
        return entity.entityTags().contains(GHOST_TAG);
    }

    private static void clearReplays() {
        restoreHudState();
        restoreFirstPersonHandState();
        if (activeLevel != null) {
            for (final ActiveReplay ghost : ACTIVE_GHOSTS) {
                ghost.remove(activeLevel);
            }
            if (activeFirstPerson != null) {
                activeFirstPerson.remove(activeLevel);
            }
        }
        ACTIVE_GHOSTS.clear();
        activeFirstPerson = null;
    }

    private record FirstPersonHandState(ItemStack playerMainHand,
                                        ItemStack playerOffHand,
                                        ItemStack rendererMainHand,
                                        ItemStack rendererOffHand) {
    }

    private record ReplayFrame(EntitySnapshot snapshot,
                               Vec3 position,
                               float yRot,
                               float xRot,
                               float yBodyRot,
                               float yHeadRot) {
    }

    private static final class ActiveReplay {
        private final LivingEntity entity;
        private final List<ReplayFrame> frames;
        private final List<EntityReplayEvent> entityEvents;
        private final int replayId;
        private final UUID replayUuid;
        private final boolean asExternalGhost;
        private final Entity previousCameraEntity;
        private final CameraType previousCameraType;
        private final long firstGameTime;
        private final List<ScheduledOutsideReplay> scheduledOutsideReplays;
        private final List<ActiveOutsideReplay> outsideReplays = new ArrayList<>();

        private int nextFrame;
        private int nextEntityEvent;
        private int nextOutsideReplay;
        private long elapsedTicks;
        private int endHoldTicks;

        private ActiveReplay(final LivingEntity entity,
                             final List<ReplayFrame> frames,
                             final List<EntityReplayEvent> entityEvents,
                             final int replayId,
                             final UUID replayUuid,
                             final boolean asExternalGhost,
                             final Entity previousCameraEntity,
                             final CameraType previousCameraType) {
            this.entity = entity;
            this.frames = List.copyOf(frames);
            this.entityEvents = List.copyOf(entityEvents);
            this.replayId = replayId;
            this.replayUuid = replayUuid;
            this.asExternalGhost = asExternalGhost;
            this.previousCameraEntity = previousCameraEntity;
            this.previousCameraType = previousCameraType;
            this.firstGameTime = this.frames.getFirst().snapshot().gameTime();
            this.scheduledOutsideReplays = asExternalGhost
                ? List.of()
                : this.buildOutsideReplaySchedule();
            this.nextFrame = 1;
            this.elapsedTicks = 1L;
        }

        private LivingEntity entity() {
            return this.entity;
        }

        private EntitySnapshot hudSnapshot() {
            return this.frames.get(Math.max(0, this.nextFrame - 1)).snapshot();
        }

        private double visualTime(final double partialTick) {
            return this.elapsedTicks + this.endHoldTicks + partialTick;
        }

        private boolean tick(final ClientLevel level) {
            if (this.entity.isRemoved() || this.entity.level() != level) {
                return false;
            }

            this.tickOutsideEntities(level, this.elapsedTicks);

            final Vec3 previousPosition = this.entity.position();
            final float previousYRot = this.entity.getYRot();
            final float previousXRot = this.entity.getXRot();
            final float previousYBodyRot = this.entity.yBodyRot;
            final float previousYHeadRot = this.entity.yHeadRot;

            while (this.nextFrame < this.frames.size() && this.frameOffset(this.nextFrame) <= this.elapsedTicks) {
                applySnapshotState(this.entity, this.frames.get(this.nextFrame), this.replayId, this.replayUuid, this.asExternalGhost);
                this.nextFrame++;
            }

            final ReplayFrame currentFrame = this.frames.get(this.nextFrame - 1);
            if (this.nextFrame < this.frames.size()) {
                applyInterpolatedFrameTransform(
                    this.entity,
                    currentFrame,
                    this.frames.get(this.nextFrame),
                    this.frameOffset(this.nextFrame - 1),
                    this.frameOffset(this.nextFrame),
                    this.elapsedTicks
                );
            } else {
                applyFrameTransform(this.entity, currentFrame);
            }

            this.entity.setOldPosAndRot(previousPosition, previousYRot, previousXRot);
            this.entity.yBodyRotO = previousYBodyRot;
            this.entity.yHeadRotO = previousYHeadRot;
            applyReplayState(this.entity, this.replayId, this.replayUuid, this.asExternalGhost);
            this.replayEventsThrough(this.elapsedTicks);
            this.entity.calculateEntityAnimation(this.entity instanceof FlyingAnimal);
            this.enforceCamera();

            if (this.nextFrame < this.frames.size()
                || this.nextEntityEvent < this.entityEvents.size()
                || this.nextOutsideReplay < this.scheduledOutsideReplays.size()
                || !this.outsideReplays.isEmpty()) {
                this.elapsedTicks++;
                return true;
            }

            return this.endHoldTicks++ < END_HOLD_TICKS;
        }

        private void enforceCamera() {
            if (this.asExternalGhost) {
                return;
            }

            final Minecraft minecraft = Minecraft.getInstance();
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            if (minecraft.getCameraEntity() != this.entity) {
                minecraft.setCameraEntity(this.entity);
            }
        }

        private void replayEventsThrough(final long elapsedTicks) {
            while (this.nextEntityEvent < this.entityEvents.size() && this.entityEventOffset(this.nextEntityEvent) <= elapsedTicks) {
                this.entity.handleEntityEvent(this.entityEvents.get(this.nextEntityEvent).eventByte());
                this.nextEntityEvent++;
            }
        }

        private List<ScheduledOutsideReplay> buildOutsideReplaySchedule() {
            final List<ScheduledOutsideReplay> schedule = new ArrayList<>();
            for (final EntityReplayEvent replayEvent : this.entityEvents) {
                final long eventOffset = Math.max(0L, replayEvent.gameTime() - this.firstGameTime);
                replayEvent.sourceEntity().ifPresent(sourceReplay -> {
                    final long duration = sourceReplay.durationTicks();
                    schedule.add(new ScheduledOutsideReplay(
                        eventOffset,
                        eventOffset + duration,
                        0L,
                        sourceReplay
                    ));
                });
            }
            schedule.sort(Comparator.comparingLong(ScheduledOutsideReplay::startOffset));
            return List.copyOf(schedule);
        }

        private void tickOutsideEntities(final ClientLevel level, final long parentElapsedTicks) {
            if (this.asExternalGhost) {
                return;
            }

            while (this.nextOutsideReplay < this.scheduledOutsideReplays.size()
                && this.scheduledOutsideReplays.get(this.nextOutsideReplay).startOffset() <= parentElapsedTicks) {
                this.startOutsideReplay(level, this.scheduledOutsideReplays.get(this.nextOutsideReplay));
                this.nextOutsideReplay++;
            }

            final Iterator<ActiveOutsideReplay> iterator = this.outsideReplays.iterator();
            while (iterator.hasNext()) {
                final ActiveOutsideReplay replay = iterator.next();
                if (!replay.tick(level, parentElapsedTicks)) {
                    replay.remove(level);
                    iterator.remove();
                }
            }
        }

        private void startOutsideReplay(final ClientLevel level, final ScheduledOutsideReplay scheduledReplay) {
            final OutsideEntityReplay sourceReplay = scheduledReplay.replay();
            final UUID sourceEntityId = sourceReplay.sourceEntity().uuid();

            final Iterator<ActiveOutsideReplay> iterator = this.outsideReplays.iterator();
            while (iterator.hasNext()) {
                final ActiveOutsideReplay replay = iterator.next();
                if (replay.sourceEntityId().equals(sourceEntityId)) {
                    replay.remove(level);
                    iterator.remove();
                }
            }

            final Identifier entityTypeId = Identifier.tryParse(sourceReplay.sourceEntity().type());
            if (entityTypeId == null) {
                return;
            }

            final Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId);
            if (entityType.isEmpty()) {
                return;
            }

            final LivingEntity replayEntity = createReplayEntity(level, entityType.get(), sourceEntityId);
            if (replayEntity == null) {
                return;
            }

            final int replayId = NEXT_ENTITY_ID.getAndDecrement();
            final UUID replayUuid = UUID.randomUUID();
            final List<ReplayFrame> replayFrames = buildReplayFrames(replayEntity, sourceReplay.snapshots());
            applyInitialFrame(replayEntity, replayFrames.getFirst(), replayId, replayUuid, true);
            level.addEntity(replayEntity);
            this.outsideReplays.add(new ActiveOutsideReplay(
                sourceEntityId,
                replayEntity,
                replayFrames,
                sourceReplay.entityEvents(),
                replayId,
                replayUuid,
                scheduledReplay.startOffset(),
                scheduledReplay.endOffset(),
                scheduledReplay.sourceStartOffset()
            ));
        }

        private void removeOutsideEntities(final ClientLevel level) {
            for (final ActiveOutsideReplay replay : this.outsideReplays) {
                replay.remove(level);
            }
            this.outsideReplays.clear();
        }

        private long frameOffset(final int index) {
            return Math.max(0L, this.frames.get(index).snapshot().gameTime() - this.firstGameTime);
        }

        private long entityEventOffset(final int index) {
            return Math.max(0L, this.entityEvents.get(index).gameTime() - this.firstGameTime);
        }

        private void remove(final ClientLevel level) {
            this.removeOutsideEntities(level);
            if (!this.asExternalGhost) {
                restoreHudState();
                restoreFirstPersonHandState();
                final Minecraft minecraft = Minecraft.getInstance();
                if (this.previousCameraType != null) {
                    minecraft.options.setCameraType(this.previousCameraType);
                }
                final Entity fallback = minecraft.player;
                final Entity camera = this.previousCameraEntity != null
                    && !this.previousCameraEntity.isRemoved()
                    && this.previousCameraEntity.level() == minecraft.level
                    ? this.previousCameraEntity
                    : fallback;
                minecraft.setCameraEntity(camera);
            }

            if (!this.entity.isRemoved()) {
                level.removeEntity(this.entity.getId(), Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private record ScheduledOutsideReplay(long startOffset,
                                          long endOffset,
                                          long sourceStartOffset,
                                          OutsideEntityReplay replay) {
    }

    private static final class ActiveOutsideReplay {
        private final UUID sourceEntityId;
        private final LivingEntity entity;
        private final List<ReplayFrame> frames;
        private final List<OutsideEntityEvent> entityEvents;
        private final int replayId;
        private final UUID replayUuid;
        private final long firstGameTime;
        private final long startOffset;
        private final long endOffset;
        private final long sourceStartOffset;
        private int nextFrame = 1;
        private int nextEntityEvent;

        private ActiveOutsideReplay(final UUID sourceEntityId,
                                    final LivingEntity entity,
                                    final List<ReplayFrame> frames,
                                    final List<OutsideEntityEvent> entityEvents,
                                    final int replayId,
                                    final UUID replayUuid,
                                    final long startOffset,
                                    final long endOffset,
                                    final long sourceStartOffset) {
            this.sourceEntityId = sourceEntityId;
            this.entity = entity;
            this.frames = List.copyOf(frames);
            this.entityEvents = List.copyOf(entityEvents);
            this.replayId = replayId;
            this.replayUuid = replayUuid;
            this.firstGameTime = this.frames.getFirst().snapshot().gameTime();
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.sourceStartOffset = sourceStartOffset;
        }

        private UUID sourceEntityId() {
            return this.sourceEntityId;
        }

        private boolean tick(final ClientLevel level, final long parentElapsedTicks) {
            if (this.entity.isRemoved() || this.entity.level() != level || parentElapsedTicks > this.endOffset) {
                return false;
            }

            final long replayElapsedTicks = this.sourceStartOffset + Math.max(0L, parentElapsedTicks - this.startOffset);
            final Vec3 previousPosition = this.entity.position();
            final float previousYRot = this.entity.getYRot();
            final float previousXRot = this.entity.getXRot();
            final float previousYBodyRot = this.entity.yBodyRot;
            final float previousYHeadRot = this.entity.yHeadRot;

            while (this.nextFrame < this.frames.size()
                && this.frameOffset(this.nextFrame) <= replayElapsedTicks) {
                applySnapshotState(
                    this.entity,
                    this.frames.get(this.nextFrame),
                    this.replayId,
                    this.replayUuid,
                    true
                );
                this.nextFrame++;
            }

            final ReplayFrame currentFrame = this.frames.get(this.nextFrame - 1);
            if (this.nextFrame < this.frames.size()) {
                applyInterpolatedFrameTransform(
                    this.entity,
                    currentFrame,
                    this.frames.get(this.nextFrame),
                    this.frameOffset(this.nextFrame - 1),
                    this.frameOffset(this.nextFrame),
                    replayElapsedTicks
                );
            } else {
                applyFrameTransform(this.entity, currentFrame);
            }

            this.entity.setOldPosAndRot(previousPosition, previousYRot, previousXRot);
            this.entity.yBodyRotO = previousYBodyRot;
            this.entity.yHeadRotO = previousYHeadRot;
            applyReplayState(this.entity, this.replayId, this.replayUuid, true);
            this.replayEventsThrough(replayElapsedTicks);
            this.entity.calculateEntityAnimation(this.entity instanceof FlyingAnimal);

            return true;
        }

        private void replayEventsThrough(final long elapsedTicks) {
            while (this.nextEntityEvent < this.entityEvents.size() && this.entityEventOffset(this.nextEntityEvent) <= elapsedTicks) {
                this.entity.handleEntityEvent(this.entityEvents.get(this.nextEntityEvent).eventByte());
                this.nextEntityEvent++;
            }
        }

        private long frameOffset(final int index) {
            return Math.max(0L, this.frames.get(index).snapshot().gameTime() - this.firstGameTime);
        }

        private long entityEventOffset(final int index) {
            return Math.max(0L, this.entityEvents.get(index).gameTime() - this.firstGameTime);
        }

        private void remove(final ClientLevel level) {
            if (!this.entity.isRemoved()) {
                level.removeEntity(this.entity.getId(), Entity.RemovalReason.DISCARDED);
            }
        }
    }
}
