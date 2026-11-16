package com.dropzone.client;

import com.dropzone.hammer.CarvedData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders a carved block by cutting the original block's own quads into the remaining 2x2x2 pieces.
 * Each piece keeps exactly the part of the texture it had in the full block, so the texture stays continuous
 * and is "split where it cuts". Faces opened up inside the block reuse the matching outer face's texture at
 * the same position, the way vanilla stairs do.
 */
public class CarvedBakedModel implements IDynamicBakedModel {
    private static final int STRIDE = 8; // DefaultVertexFormat.BLOCK: pos(3) color uv(2) light normal

    private final BakedModel fallback;
    private final Map<Key, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    private record Key(BlockState source, int mask, @Nullable RenderType type, @Nullable Direction side) {}

    public CarvedBakedModel(BakedModel fallback) {
        this.fallback = fallback;
    }

    private static BakedModel sourceModel(BlockState source) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(source);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        CarvedData carved = data.get(CarvedData.PROPERTY);
        if (carved == null) return fallback.getQuads(state, side, rand, data, renderType);
        if (cache.size() > 4096) cache.clear();
        return cache.computeIfAbsent(new Key(carved.source(), carved.mask(), renderType, side), k -> build(k.source, k.mask, k.type, k.side));
    }

    private static List<BakedQuad> build(BlockState source, int mask, @Nullable RenderType type, @Nullable Direction side) {
        BakedModel model = sourceModel(source);
        RandomSource rand = RandomSource.create(42L);
        List<BakedQuad> out = new ArrayList<>();
        List<BakedQuad> general = model.getQuads(source, null, rand, ModelData.EMPTY, type);

        for (Direction dir : Direction.values()) {
            rand.setSeed(42L);
            List<BakedQuad> faceQuads = new ArrayList<>(model.getQuads(source, dir, rand, ModelData.EMPTY, type));
            for (BakedQuad q : general) if (q.getDirection() == dir) faceQuads.add(q);
            if (faceQuads.isEmpty()) continue;

            int axis = dir.getAxis().ordinal(); // X=0, Y=1, Z=2
            int ia = axis == 0 ? 1 : 0;
            int ib = axis == 2 ? 1 : 2;
            boolean positive = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE;

            for (int i = 0; i < 8; i++) {
                if ((mask & (1 << i)) == 0) continue;
                int[] o = {i & 1, (i >> 1) & 1, (i >> 2) & 1};
                double plane = o[axis] * 0.5D + (positive ? 0.5D : 0.0D);
                boolean boundary = plane <= 0.0D || plane >= 1.0D;
                // Outer faces go in the per-side lists so the game can cull them against neighbours.
                if (boundary ? side != dir : side != null) continue;
                if (!boundary) {
                    int[] n = o.clone();
                    n[axis] += positive ? 1 : -1;
                    int neighbour = n[0] | (n[1] << 1) | (n[2] << 2);
                    if ((mask & (1 << neighbour)) != 0) continue; // hidden inside the block
                }
                double a0 = o[ia] * 0.5D, b0 = o[ib] * 0.5D;
                for (BakedQuad q : faceQuads) {
                    BakedQuad cut = clip(q, axis, ia, ib, plane, a0, a0 + 0.5D, b0, b0 + 0.5D);
                    if (cut != null) out.add(cut);
                }
            }
        }
        return out;
    }

    /** Cuts {@code q} down to the rectangle [a0,a1] x [b0,b1] on the given plane, remapping UVs to match. */
    @Nullable
    private static BakedQuad clip(BakedQuad q, int axis, int ia, int ib, double plane, double a0, double a1, double b0, double b1) {
        int[] src = q.getVertices();
        if (src.length < STRIDE * 4) return null;
        double[][] p = new double[4][3];
        double[] u = new double[4], v = new double[4];
        double minA = 1, maxA = 0, minB = 1, maxB = 0;
        for (int i = 0; i < 4; i++) {
            for (int k = 0; k < 3; k++) p[i][k] = Float.intBitsToFloat(src[i * STRIDE + k]);
            u[i] = Float.intBitsToFloat(src[i * STRIDE + 4]);
            v[i] = Float.intBitsToFloat(src[i * STRIDE + 5]);
            minA = Math.min(minA, p[i][ia]); maxA = Math.max(maxA, p[i][ia]);
            minB = Math.min(minB, p[i][ib]); maxB = Math.max(maxB, p[i][ib]);
        }
        double ca0 = Math.max(a0, minA), ca1 = Math.min(a1, maxA);
        double cb0 = Math.max(b0, minB), cb1 = Math.min(b1, maxB);
        if (ca1 - ca0 < 1.0E-5D || cb1 - cb0 < 1.0E-5D) return null;

        // The texture on a face quad is an affine function of the two in-plane coordinates.
        double[] cu = affine(p, u, ia, ib);
        double[] cv = affine(p, v, ia, ib);
        if (cu == null || cv == null) return null;

        int[] out = src.clone();
        for (int i = 0; i < 4; i++) {
            double a = Math.max(ca0, Math.min(ca1, p[i][ia]));
            double b = Math.max(cb0, Math.min(cb1, p[i][ib]));
            double[] pos = new double[3];
            pos[axis] = plane;
            pos[ia] = a;
            pos[ib] = b;
            for (int k = 0; k < 3; k++) out[i * STRIDE + k] = Float.floatToRawIntBits((float) pos[k]);
            out[i * STRIDE + 4] = Float.floatToRawIntBits((float) (cu[0] + cu[1] * a + cu[2] * b));
            out[i * STRIDE + 5] = Float.floatToRawIntBits((float) (cv[0] + cv[1] * a + cv[2] * b));
        }
        return new BakedQuad(out, q.getTintIndex(), q.getDirection(), q.getSprite(), q.isShade());
    }

    /** Solves t = c0 + c1*A + c2*B from three corners of the quad. */
    @Nullable
    private static double[] affine(double[][] p, double[] t, int ia, int ib) {
        int[][] tries = {{0, 1, 2}, {0, 1, 3}, {0, 2, 3}, {1, 2, 3}};
        for (int[] c : tries) {
            double A0 = p[c[0]][ia], B0 = p[c[0]][ib];
            double A1 = p[c[1]][ia], B1 = p[c[1]][ib];
            double A2 = p[c[2]][ia], B2 = p[c[2]][ib];
            double det = (A1 - A0) * (B2 - B0) - (A2 - A0) * (B1 - B0);
            if (Math.abs(det) < 1.0E-9D) continue;
            double dt1 = t[c[1]] - t[c[0]], dt2 = t[c[2]] - t[c[0]];
            double c1 = (dt1 * (B2 - B0) - dt2 * (B1 - B0)) / det;
            double c2 = ((A1 - A0) * dt2 - (A2 - A0) * dt1) / det;
            double c0 = t[c[0]] - c1 * A0 - c2 * B0;
            return new double[]{c0, c1, c2};
        }
        return null;
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        CarvedData carved = data.get(CarvedData.PROPERTY);
        if (carved == null) return fallback.getRenderTypes(state, rand, data);
        return sourceModel(carved.source()).getRenderTypes(carved.source(), rand, ModelData.EMPTY);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        CarvedData carved = data.get(CarvedData.PROPERTY);
        return carved != null ? sourceModel(carved.source()).getParticleIcon(ModelData.EMPTY) : fallback.getParticleIcon();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public TextureAtlasSprite getParticleIcon() {
        return fallback.getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
