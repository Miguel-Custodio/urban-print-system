package com.urbanprint.backend.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CutPlanService {

    public record Piece(double x, double y, double w, double h, boolean rotated) {}

    public record Plan(double sheetW, double sheetH, double pieceW, double pieceH,
                       int perSheet, int sheetsNeeded, double utilizationPct, double wastePct,
                       List<Piece> pieces, String note) {}

    private static final int U = 8; // working unit = 1/8 inch

    public Plan plan(double sheetW, double sheetH, double pieceW, double pieceH,
                     int qty, double gap, double margin, boolean rotate) {
        if (sheetW <= 0 || sheetH <= 0 || pieceW <= 0 || pieceH <= 0)
            throw new IllegalArgumentException("Sheet and piece dimensions must be positive");
        if (qty < 1) qty = 1;

        int gU = (int) Math.round(gap * U);
        int pw = (int) Math.ceil(pieceW * U) + gU;
        int ph = (int) Math.ceil(pieceH * U) + gU;
        int W = (int) Math.floor((sheetW - 2 * margin) * U) + gU;
        int H = (int) Math.floor((sheetH - 2 * margin) * U) + gU;

        List<int[]> rects = new ArrayList<>();
        String note = null;

        if (W > 0 && H > 0) {
            long ratio = ((long) W * H) / ((long) pw * ph);
            if (ratio <= 40) {
                Solver s = new Solver(pw, ph, rotate);
                s.solve(W, H);
                s.place(W, H, 0, 0, rects);
            } else {
                // Many small pieces: use fast grid (best orientation)
                gridFallback(W, H, pw, ph, rotate, rects);
                note = "Many pieces per sheet: simple grid layout used";
            }
        }

        List<Piece> pieces = new ArrayList<>();
        for (int[] r : rects) {
            pieces.add(new Piece(r[0] / (double) U + margin, r[1] / (double) U + margin,
                    (r[2] - gU) / (double) U, (r[3] - gU) / (double) U, r[4] == 1));
        }

        int perSheet = pieces.size();
        double used = perSheet * pieceW * pieceH;
        double util = perSheet == 0 ? 0 : used / (sheetW * sheetH) * 100.0;
        int sheets = perSheet == 0 ? 0 : (int) Math.ceil((double) qty / perSheet);
        if (perSheet == 0) note = "The piece does not fit on this sheet";

        return new Plan(sheetW, sheetH, pieceW, pieceH, perSheet, sheets,
                Math.round(util * 10) / 10.0, Math.round((100 - util) * 10) / 10.0, pieces, note);
    }

    private void gridFallback(int W, int H, int pw, int ph, boolean rotate, List<int[]> out) {
        int c1 = (W / pw) * (H / ph);
        int c2 = rotate ? (W / ph) * (H / pw) : 0;
        boolean rot = c2 > c1;
        int w = rot ? ph : pw, h = rot ? pw : ph;
        for (int i = 0; i < W / w; i++)
            for (int j = 0; j < H / h; j++)
                out.add(new int[]{i * w, j * h, w, h, rot ? 1 : 0});
    }

    /** Guillotine-cut optimiser (recursive with memoization). */
    private static class Solver {
        final int pw, ph; final boolean rot;
        final Map<Long, int[]> memo = new HashMap<>();
        final Map<Integer, int[]> cutCache = new HashMap<>();

        Solver(int pw, int ph, boolean rot) { this.pw = pw; this.ph = ph; this.rot = rot; }

        int[] cuts(int dim) {
            return cutCache.computeIfAbsent(dim, d -> {
                TreeSet<Integer> set = new TreeSet<>();
                for (int a = 0; a * pw < d; a++)
                    for (int b = 0; a * pw + b * ph < d; b++) {
                        int v = a * pw + b * ph;
                        if (v > 0) set.add(v);
                    }
                return set.stream().mapToInt(Integer::intValue).toArray();
            });
        }

        // result: [count, type(0 none,1 piece,2 vertical cut,3 horizontal cut), cutPos]
        int[] solve(int W, int H) {
            long key = (long) W * 1_000_000L + H;
            int[] m = memo.get(key);
            if (m != null) return m;
            int[] best = new int[]{0, 0, 0};
            boolean fu = pw <= W && ph <= H;
            boolean fr = rot && ph <= W && pw <= H;
            if (!fu && !fr) { memo.put(key, best); return best; }
            best[0] = 1; best[1] = 1;
            for (int x : cuts(W)) {
                int c = solve(x, H)[0] + solve(W - x, H)[0];
                if (c > best[0]) { best[0] = c; best[1] = 2; best[2] = x; }
            }
            for (int y : cuts(H)) {
                int c = solve(W, y)[0] + solve(W, H - y)[0];
                if (c > best[0]) { best[0] = c; best[1] = 3; best[2] = y; }
            }
            memo.put(key, best);
            return best;
        }

        void place(int W, int H, int x0, int y0, List<int[]> out) {
            int[] r = solve(W, H);
            if (r[0] == 0) return;
            switch (r[1]) {
                case 1 -> {
                    boolean fu = pw <= W && ph <= H;
                    out.add(new int[]{x0, y0, fu ? pw : ph, fu ? ph : pw, fu ? 0 : 1});
                }
                case 2 -> { place(r[2], H, x0, y0, out); place(W - r[2], H, x0 + r[2], y0, out); }
                case 3 -> { place(W, r[2], x0, y0, out); place(W, H - r[2], x0, y0 + r[2], out); }
                default -> {}
            }
        }
    }
}
