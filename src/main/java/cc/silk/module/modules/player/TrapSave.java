package cc.silk.module.modules.player;

import com.mojang.blaze3d.systems.RenderSystem;
import cc.silk.event.impl.player.TickEvent;
import cc.silk.event.impl.render.Render2DEvent;
import cc.silk.event.impl.render.Render3DEvent;
import cc.silk.utils.render.RenderCompat;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.render.font.util.RendererUtils;
import cc.silk.utils.math.TimerUtil;
import cc.silk.utils.mc.ChatUtil;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.*;
import com.mojang.blaze3d.vertex.*;
import cc.silk.utils.render.CompatShaders;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;

public final class TrapSave extends Module {
    private static final int SCAN_INTERVAL_MS = 500;
    private static final int WARNING_DURATION_MS = 3000;
    private static final String CHAT_PREFIX = "§c[TrapSave] §f";

    private static final AABB REDSTONE_WIRE_BOUNDS = new AABB(0, 0, 0, 1, 0.0625, 1);
    private static final AABB TORCH_BOUNDS = new AABB(0.375, 0, 0.375, 0.625, 0.625, 0.625);
    private static final AABB LEVER_BOUNDS = new AABB(0.25, 0, 0.25, 0.75, 0.6, 0.75);
    private static final AABB PRESSURE_PLATE_BOUNDS = new AABB(0.0625, 0, 0.0625, 0.9375, 0.0625, 0.9375);
    private static final AABB TRIPWIRE_HOOK_BOUNDS = new AABB(0.375, 0, 0.125, 0.625, 0.625, 0.875);
    private static final AABB THIN_BLOCK_BOUNDS = new AABB(0, 0, 0, 1, 0.125, 1);
    private static final AABB FULL_BLOCK_BOUNDS = new AABB(0, 0, 0, 1, 1, 1);
    private static final AABB ARMOR_STAND_BOUNDS = new AABB(-0.5, 0, -0.5, 0.5, 1.975, 0.5);

    private static final Predicate<ArmorStand> ARMOR_STAND_FILTER = Objects::nonNull;

    private final NumberSetting scanRadius = new NumberSetting("Scan Radius", 3, 20, 8, 1);
    private final NumberSetting scanHeight = new NumberSetting("Scan Height", 0, 30, 8, 1);
    private final NumberSetting maxExpansion = new NumberSetting("Max Expansion", 0, 100, 20, 1);
    private final BooleanSetting soundAlert = new BooleanSetting("Sound Alert", true);
    private final BooleanSetting showWarning = new BooleanSetting("Show Warning", true);
    private final NumberSetting outlineWidth = new NumberSetting("Outline Width", 1, 5, 2, 1);
    private final NumberSetting tntColor = new NumberSetting("TNT Color", 0, 16777215, 0xFFA500, 1);
    private final NumberSetting redstoneColor = new NumberSetting("Redstone Color", 0, 16777215, 0xFF0000, 1);
    private final NumberSetting pistonColor = new NumberSetting("Piston Color", 0, 16777215, 0x8B4513, 1);
    private final NumberSetting leverColor = new NumberSetting("Lever Color", 0, 16777215, 0xFFFF00, 1);
    private final NumberSetting armorStandColor = new NumberSetting("Armor Stand Color", 0, 16777215, 0x00FFFF, 1);

    private final TimerUtil scanTimer = new TimerUtil();
    private final TimerUtil warningTimer = new TimerUtil();
    private final List<TrapCluster> detectedTraps = new ArrayList<>();
    private boolean trapDetected;
    private String detectedTrapType = "";
    private int trapCount;
    private BlockPos lastScanPosition;
    private int lastScanRadius;
    private int lastScanHeight;

    public TrapSave() {
        super("Trap Save", "Detects trap blocks and armor stands around the player", -1, Category.PLAYER);
        this.addSettings(scanRadius, scanHeight, maxExpansion, soundAlert, showWarning, outlineWidth,
                tntColor, redstoneColor, pistonColor, leverColor, armorStandColor);
    }

    @EventHandler
    private void onEventRender3D(Render3DEvent event) {
        if (isValidGameState() || detectedTraps.isEmpty()) return;

        renderTrapOutlines(event.getMatrixStack(), mc.gameRenderer.getMainCamera().position());
    }

    @EventHandler
    private void onEventRender2D(Render2DEvent event) {
        if (isValidGameState() || !showWarning.getValue() || !trapDetected) return;

        renderWarningOverlay(event);
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isValidGameState()) return;

        if (scanTimer.hasElapsedTime(SCAN_INTERVAL_MS)) {
            performTrapScan();
            scanTimer.reset();
        }

        if (trapDetected && warningTimer.hasElapsedTime(WARNING_DURATION_MS)) {
            resetTrapDetection();
        }
    }

    private boolean isValidGameState() {
        return mc.player == null || mc.level == null || !isEnabled();
    }

    private void performTrapScan() {
        final BlockPos playerPos = mc.player.blockPosition();
        final int radius = scanRadius.getValueInt();
        final int height = scanHeight.getValueInt();

        if (shouldSkipScan(playerPos, radius, height)) return;

        clearDetectedTraps();
        final List<TrapCluster> foundTraps = scanForTrapClusters(playerPos, radius, height);

        updateScanCache(playerPos, radius, height);

        if (!foundTraps.isEmpty() && !trapDetected) {
            handleTrapDetection(foundTraps);
        }
    }

    private boolean shouldSkipScan(BlockPos currentPos, int currentRadius, int currentHeight) {
        return lastScanPosition != null &&
                lastScanPosition.equals(currentPos) &&
                lastScanRadius == currentRadius &&
                lastScanHeight == currentHeight;
    }

    private void updateScanCache(BlockPos position, int radius, int height) {
        lastScanPosition = position;
        lastScanRadius = radius;
        lastScanHeight = height;
    }

    private List<TrapCluster> scanForTrapClusters(BlockPos playerPos, int radius, int height) {
        final Set<BlockPos> scannedBlocks = new HashSet<>();
        final List<TrapCluster> clusters = new ArrayList<>();
        final int radiusSquared = radius * radius;
        final boolean infiniteHeight = height == 0;
        final int minY = infiniteHeight ? mc.level.getMinY() : playerPos.getY() - height;
        final int maxY = infiniteHeight ? mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, playerPos.getX(), playerPos.getZ()) - 1 : playerPos.getY() + height;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radiusSquared) continue;

                for (int y = minY; y <= maxY; y++) {
                    final BlockPos scanPos = new BlockPos(playerPos.getX() + x, y, playerPos.getZ() + z);

                    if (scannedBlocks.contains(scanPos)) continue;

                    final Block block = mc.level.getBlockState(scanPos).getBlock();
                    final TrapType trapType = getTrapType(block);

                    if (trapType != TrapType.NONE) {
                        final TrapCluster cluster = expandTrapCluster(scanPos, trapType, scannedBlocks);
                        if (!cluster.isEmpty()) {
                            clusters.add(cluster);
                        }
                    }
                }
            }
        }

        scanArmorStands(playerPos, radius, height, clusters);
        detectedTraps.addAll(clusters);

        return clusters;
    }

    private TrapCluster expandTrapCluster(BlockPos startPos, TrapType startType, Set<BlockPos> globalScanned) {
        final TrapCluster cluster = new TrapCluster(startType);
        final Queue<BlockPos> toExpand = new ArrayDeque<>();
        final Set<BlockPos> clusterScanned = new HashSet<>();
        final int maxBlocks = maxExpansion.getValueInt();
        final boolean infiniteExpansion = maxBlocks == 0;
        final int safetyLimit = 1000;

        toExpand.offer(startPos);

        while (!toExpand.isEmpty() &&
                (infiniteExpansion ? cluster.size() < safetyLimit : cluster.size() < maxBlocks)) {
            final BlockPos current = toExpand.poll();

            if (clusterScanned.contains(current) || globalScanned.contains(current)) continue;

            clusterScanned.add(current);
            globalScanned.add(current);

            final Block block = mc.level.getBlockState(current).getBlock();
            final TrapType trapType = getTrapType(block);

            if (trapType != TrapType.NONE && isRelatedTrapType(startType, trapType)) {
                cluster.addBlock(current, trapType);

                assert current != null;
                for (BlockPos neighbor : getNeighbors(current)) {
                    if (!clusterScanned.contains(neighbor)) {
                        toExpand.offer(neighbor);
                    }
                }
            }
        }

        return cluster;
    }

    private boolean isRelatedTrapType(TrapType original, TrapType candidate) {
        if (original == candidate) return true;

        final Set<TrapType> redstoneGroup = EnumSet.of(TrapType.REDSTONE, TrapType.LEVER, TrapType.PRESSURE_PLATE, TrapType.TRIPWIRE);
        final Set<TrapType> mechanicalGroup = EnumSet.of(TrapType.PISTON, TrapType.DISPENSER, TrapType.TNT);

        if (redstoneGroup.contains(original) && redstoneGroup.contains(candidate)) return true;
        if (mechanicalGroup.contains(original) && mechanicalGroup.contains(candidate)) return true;
        if (redstoneGroup.contains(original) && mechanicalGroup.contains(candidate)) return true;
        return mechanicalGroup.contains(original) && redstoneGroup.contains(candidate);
    }

    private List<BlockPos> getNeighbors(BlockPos pos) {
        return Arrays.asList(
                pos.north(), pos.south(), pos.east(), pos.west(),
                pos.above(), pos.below(),
                pos.north().above(), pos.south().above(), pos.east().above(), pos.west().above(),
                pos.north().below(), pos.south().below(), pos.east().below(), pos.west().below()
        );
    }

    private void scanArmorStands(BlockPos playerPos, int radius, int height, List<TrapCluster> clusters) {
        final boolean infiniteHeight = height == 0;
        final int minY = infiniteHeight ? mc.level.getMinY() : playerPos.getY() - height;
        final int maxY = infiniteHeight ? mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, playerPos.getX(), playerPos.getZ()) - 1 : playerPos.getY() + height;

        final Vec3 minPos = new Vec3(playerPos.getX() - radius, minY, playerPos.getZ() - radius);
        final Vec3 maxPos = new Vec3(playerPos.getX() + radius, maxY, playerPos.getZ() + radius);
        final List<ArmorStand> armorStands = mc.level.getEntities(EntityTypeTest.forClass(ArmorStand.class), new AABB(minPos, maxPos), ARMOR_STAND_FILTER);

        if (!armorStands.isEmpty()) {
            final TrapCluster armorStandCluster = new TrapCluster(TrapType.ARMOR_STAND);
            armorStands.forEach(armorStandCluster::addArmorStand);
            clusters.add(armorStandCluster);
        }
    }

    private TrapType getTrapType(Block block) {
        if (block == Blocks.TNT) return TrapType.TNT;
        if (block == Blocks.LEVER) return TrapType.LEVER;
        if (block == Blocks.PISTON || block == Blocks.STICKY_PISTON) return TrapType.PISTON;
        if (isRedstoneBlock(block)) return TrapType.REDSTONE;
        if (isPressurePlate(block)) return TrapType.PRESSURE_PLATE;
        if (block == Blocks.TRIPWIRE_HOOK) return TrapType.TRIPWIRE;
        if (block == Blocks.DISPENSER || block == Blocks.DROPPER) return TrapType.DISPENSER;
        return TrapType.NONE;
    }

    private boolean isRedstoneBlock(Block block) {
        return block == Blocks.REDSTONE_WIRE || block == Blocks.REDSTONE_TORCH ||
                block == Blocks.REDSTONE_WALL_TORCH || block == Blocks.REDSTONE_BLOCK ||
                block == Blocks.OBSERVER || block == Blocks.REPEATER || block == Blocks.COMPARATOR;
    }

    private boolean isPressurePlate(Block block) {
        return block == Blocks.STONE_PRESSURE_PLATE ||
                block == Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE ||
                block == Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE;
    }

    private void handleTrapDetection(List<TrapCluster> traps) {
        trapDetected = true;
        detectedTrapType = traps.getFirst().getPrimaryType().getDisplayName();
        trapCount = traps.size();
        warningTimer.reset();

        if (soundAlert.getValue()) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 2.0f));
        }

        ChatUtil.addChatMessage(CHAT_PREFIX + "Trap detected: " + detectedTrapType + " (" + trapCount + " traps)");
    }

    private void renderWarningOverlay(Render2DEvent event) {
        final int centerX = event.getWidth() / 2;
        final int warningY = 50;
        final int warningWidth = 210;
        final int warningHeight = 40;

        final int backgroundX = centerX - warningWidth / 2;
        final String warningText = "§c§lTRAP DETECTED!";
        final String detailText = "§f" + detectedTrapType + " (" + trapCount + " traps)";

        event.getContext().fill(backgroundX - 10, warningY - 10, backgroundX + warningWidth, warningY + warningHeight, 0x80FF0000);
        event.getContext().text(mc.font, warningText, backgroundX, warningY, 0xFFFFFF, true);
        event.getContext().text(mc.font, detailText, backgroundX, warningY + 15, 0xFFFFFF, true);
    }

    private void renderTrapOutlines(PoseStack matrices, Vec3 cameraPos) {
        RendererUtils.setupRender();
        
        
        
        CompatShaders.usePositionColor();

        for (TrapCluster cluster : detectedTraps) {
            renderTrapCluster(matrices, cluster, cameraPos);
        }

        
        
        RendererUtils.endRender();
    }

    private void renderTrapCluster(PoseStack matrices, TrapCluster cluster, Vec3 cameraPos) {
        final Color clusterColor = getCustomColor(cluster.getPrimaryType());

        cluster.getBlocks().forEach((blockPos, trapType) -> {
            final Block block = mc.level.getBlockState(blockPos).getBlock();
            renderBlockOutline(matrices, blockPos, block, clusterColor, cameraPos);
        });

        cluster.getArmorStands().forEach(armorStand -> renderArmorStandOutline(matrices, armorStand, clusterColor, cameraPos));
    }

    private void renderBlockOutline(PoseStack matrices, BlockPos blockPos, Block block, Color color, Vec3 cameraPos) {
        final Vec3 relativePos = getRelativePosition(blockPos, cameraPos);
        final AABB bounds = getBlockBounds(block);

        matrices.pushPose();
        matrices.translate(relativePos.x, relativePos.y, relativePos.z);
        
        renderOutlineBox(matrices, bounds, color);
        matrices.popPose();
    }

    private void renderArmorStandOutline(PoseStack matrices, ArmorStand armorStand, Color color, Vec3 cameraPos) {
        final Vec3 relativePos = getRelativePosition(armorStand.position(), cameraPos);

        matrices.pushPose();
        matrices.translate(relativePos.x, relativePos.y, relativePos.z);
        
        renderOutlineBox(matrices, ARMOR_STAND_BOUNDS, color);
        matrices.popPose();
    }

    private Color getCustomColor(TrapType trapType) {
        final int alpha = 200;
        return switch (trapType) {
            case TNT -> new Color(tntColor.getValueInt() | (alpha << 24), true);
            case REDSTONE -> new Color(redstoneColor.getValueInt() | (alpha << 24), true);
            case PISTON -> new Color(pistonColor.getValueInt() | (alpha << 24), true);
            case LEVER, PRESSURE_PLATE -> new Color(leverColor.getValueInt() | (alpha << 24), true);
            case ARMOR_STAND -> new Color(armorStandColor.getValueInt() | (alpha << 24), true);
            case TRIPWIRE -> new Color(0x800080 | (alpha << 24), true);
            case DISPENSER -> new Color(0x696969 | (alpha << 24), true);
            default -> new Color(0xFFFFFF | (alpha << 24), true);
        };
    }

    private Vec3 getRelativePosition(BlockPos blockPos, Vec3 cameraPos) {
        return new Vec3(blockPos.getX() - cameraPos.x, blockPos.getY() - cameraPos.y, blockPos.getZ() - cameraPos.z);
    }

    private Vec3 getRelativePosition(Vec3 worldPos, Vec3 cameraPos) {
        return worldPos.subtract(cameraPos);
    }

    private AABB getBlockBounds(Block block) {
        if (block == Blocks.REDSTONE_WIRE) return REDSTONE_WIRE_BOUNDS;
        if (block == Blocks.REDSTONE_TORCH || block == Blocks.REDSTONE_WALL_TORCH) return TORCH_BOUNDS;
        if (block == Blocks.LEVER) return LEVER_BOUNDS;
        if (isPressurePlate(block)) return PRESSURE_PLATE_BOUNDS;
        if (block == Blocks.TRIPWIRE_HOOK) return TRIPWIRE_HOOK_BOUNDS;
        if (block == Blocks.REPEATER || block == Blocks.COMPARATOR) return THIN_BLOCK_BOUNDS;
        return FULL_BLOCK_BOUNDS;
    }

    private void renderOutlineBox(PoseStack matrices, AABB box, Color color) {
        final Matrix4f matrix = matrices.last().pose();
        final BufferBuilder buffer = RenderCompat.begin(RenderTypes.lines());
        final float[] rgba = {color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f};
        final float[] bounds = {(float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.maxY, (float) box.maxZ};

        drawBoxEdges(buffer, matrix, bounds, rgba);
        RenderCompat.draw(buffer);
    }

    private void drawBoxEdges(BufferBuilder buffer, Matrix4f matrix, float[] bounds, float[] rgba) {
        final float minX = bounds[0], minY = bounds[1], minZ = bounds[2];
        final float maxX = bounds[3], maxY = bounds[4], maxZ = bounds[5];
        final float r = rgba[0], g = rgba[1], b = rgba[2], a = rgba[3];

        addEdge(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        addEdge(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        addEdge(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        addEdge(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        addEdge(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        addEdge(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        addEdge(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        addEdge(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        addEdge(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        addEdge(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        addEdge(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        addEdge(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private void addEdge(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        RenderCompat.lineVertex(buffer, matrix, x1, y1, z1, r, g, b, a);
        RenderCompat.lineVertex(buffer, matrix, x2, y2, z2, r, g, b, a);
    }

    private void clearDetectedTraps() {
        detectedTraps.clear();
    }

    private void resetTrapDetection() {
        trapDetected = false;
        detectedTrapType = "";
        trapCount = 0;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        clearDetectedTraps();
        resetTrapDetection();
        lastScanPosition = null;
    }

    @Override
    public void onDisable() {
        resetTrapDetection();
        clearDetectedTraps();
        lastScanPosition = null;
        super.onDisable();
    }

    @Getter
    private enum TrapType {
        NONE("", Color.WHITE),
        TNT("TNT", Color.ORANGE),
        LEVER("Lever", Color.YELLOW),
        PISTON("Piston", Color.decode("#8B4513")),
        REDSTONE("Redstone", Color.RED),
        PRESSURE_PLATE("Pressure Plate", Color.YELLOW),
        TRIPWIRE("Tripwire", Color.decode("#800080")),
        DISPENSER("Dispenser", Color.GRAY),
        ARMOR_STAND("Armor Stand", Color.CYAN);

        private final String displayName;

        TrapType(String displayName, Color defaultColor) {
            this.displayName = displayName;
        }

    }

    @Getter
    private static class TrapCluster {
        private final TrapType primaryType;
        private final Map<BlockPos, TrapType> blocks = new HashMap<>();
        private final List<ArmorStand> armorStands = new ArrayList<>();

        public TrapCluster(TrapType primaryType) {
            this.primaryType = primaryType;
        }

        public void addBlock(BlockPos pos, TrapType type) {
            blocks.put(pos, type);
        }

        public void addArmorStand(ArmorStand armorStand) {
            armorStands.add(armorStand);
        }

        public boolean isEmpty() {
            return blocks.isEmpty() && armorStands.isEmpty();
        }

        public int size() {
            return blocks.size() + armorStands.size();
        }
    }
}
