package com.example.battle_assistant.preparation;

import java.awt.image.BufferedImage;
import java.util.*;

/** Baseline fixed-layout matcher. Scores are similarity, NOT calibrated probabilities. */
public final class AvatarMatcher {
    private AvatarMatcher() {}
    public record Template(AvatarEntry entry, float[] feature) {}
    public record Match(String status, AvatarEntry identity, double score, double margin) {}

    // Use only central portrait: mask top decorations, lower level text and gender corner.
    public static float[] feature(BufferedImage image) {
        float[] out = new float[24 * 24 * 3];
        for (int y = 0; y < 24; y++) for (int x = 0; x < 24; x++) {
            int px = Math.min(image.getWidth()-1, (int)((.18 + (x+.5)/24*.64)*image.getWidth()));
            int py = Math.min(image.getHeight()-1, (int)((.18 + (y+.5)/24*.56)*image.getHeight()));
            int rgb = image.getRGB(px, py), i = (y*24+x)*3;
            out[i] = ((rgb >> 16)&255)/255f;
            out[i+1] = ((rgb >> 8)&255)/255f;
            out[i+2] = (rgb&255)/255f;
        }
        return out;
    }
    public static Match match(float[] query, List<Template> templates, double threshold, double minMargin) {
        // Multiple skins of one identity must not compete for the ambiguity margin.
        Map<String, Template> bestTemplates = new HashMap<>();
        Map<String, Double> scores = new HashMap<>();
        for (Template t : templates) {
            double error = 0;
            for (int i = 0; i < query.length; i++) error += Math.abs(query[i]-t.feature()[i]);
            double score = 1 - error/query.length;
            String key = t.entry().speciesKey()+"\n"+t.entry().form();
            if (score > scores.getOrDefault(key, -1d)) {
                scores.put(key, score); bestTemplates.put(key, t);
            }
        }
        var ranked = scores.entrySet().stream().sorted(Map.Entry.<String,Double>comparingByValue().reversed()).toList();
        if (ranked.isEmpty()) return new Match("unknown", null, 0, 0);
        double top = ranked.getFirst().getValue();
        double margin = top - (ranked.size()>1 ? ranked.get(1).getValue() : 0);
        boolean known = top >= threshold && margin >= minMargin;
        return new Match(known ? "matched" : "unknown",
            known ? bestTemplates.get(ranked.getFirst().getKey()).entry() : null, top, margin);
    }
}
