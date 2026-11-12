/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics.text;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.Rect;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Locale;
import java.util.Objects;

/**
 * Wraps a font with glyph size scaling, visual shift and code point exclusion,
 * matching vanilla TTF provider semantics (size, shift, skip).
 */
public final class AdjustedFont implements Font {

    private final Font mBase;
    private final float mScale;
    private final float mShiftX;
    private final float mShiftY;
    // null when no code point is excluded
    private final IntSet mSkip;

    public AdjustedFont(@NonNull Font base, float scale,
                        float shiftX, float shiftY, @NonNull String skip) {
        mBase = Objects.requireNonNull(base);
        mScale = scale;
        mShiftX = shiftX;
        mShiftY = shiftY;
        if (skip.isEmpty()) {
            mSkip = null;
        } else {
            IntSet set = new IntOpenHashSet();
            skip.codePoints().forEach(set::add);
            mSkip = set;
        }
    }

    @Override
    public float mapFontSize(float size) {
        // must stay consistent with FontPaint.setFontSize, see scalePaint
        return MathUtil.pin(FontPaint.getCanonicalFontSize(size * mScale), 1, 2184);
    }

    @NonNull
    public Font getBase() {
        return mBase;
    }

    private FontPaint scalePaint(@NonNull FontPaint paint) {
        FontPaint copy = new FontPaint(paint);
        copy.setFontSize(paint.getFontSize() * mScale);
        return copy;
    }

    @Override
    public int getStyle() {
        return mBase.getStyle();
    }

    @Override
    public String getFullName(@NonNull Locale locale) {
        return mBase.getFullName(locale);
    }

    @Override
    public String getFamilyName(@NonNull Locale locale) {
        return mBase.getFamilyName(locale);
    }

    @Override
    public int getMetrics(@NonNull FontPaint paint, @Nullable FontMetricsInt fm) {
        return mBase.getMetrics(scalePaint(paint), fm);
    }

    @Override
    public boolean hasGlyph(int ch, int vs) {
        return (mSkip == null || !mSkip.contains(ch)) && mBase.hasGlyph(ch, vs);
    }

    @Override
    public int calcGlyphScore(char[] buf, int start, int limit) {
        // excluded code points score as unsupported so matching falls through
        // to the next family, like vanilla providers after 'skip'
        return mSkip == null ? mBase.calcGlyphScore(buf, start, limit)
                : Font.super.calcGlyphScore(buf, start, limit);
    }

    @Override
    public float doSimpleLayout(char[] buf, int start, int limit,
                                FontPaint paint, IntArrayList glyphs,
                                FloatArrayList positions, float x, float y) {
        // shift moves glyph positions and bounds only, not advances
        return mBase.doSimpleLayout(buf, start, limit, scalePaint(paint),
                glyphs, positions, x + mShiftX, y + mShiftY);
    }

    @Override
    public float doComplexLayout(char[] buf,
                                 int contextStart, int contextLimit,
                                 int layoutStart, int layoutLimit,
                                 boolean isRtl, FontPaint paint,
                                 IntArrayList glyphs, FloatArrayList positions,
                                 float[] advances, int advanceOffset,
                                 Rect bounds, float x, float y) {
        return mBase.doComplexLayout(buf, contextStart, contextLimit,
                layoutStart, layoutLimit, isRtl, scalePaint(paint),
                glyphs, positions, advances, advanceOffset,
                bounds, x + mShiftX, y + mShiftY);
    }

    @Override
    public boolean isColorEmojiFont() {
        return mBase.isColorEmojiFont();
    }

    @Override
    public icyllis.arc3d.sketch.Typeface getNativeTypeface() {
        return mBase.getNativeTypeface();
    }

    @Override
    public boolean hasRasterGlyphs() {
        return mBase.hasRasterGlyphs();
    }

    @Override
    public void drawRasterGlyph(@NonNull Canvas canvas, int glyphId, float x, float y,
                                @NonNull Paint paint, @NonNull FontPaint fontPaint) {
        mBase.drawRasterGlyph(canvas, glyphId, x, y, paint, fontPaint);
    }

    @Override
    public String toString() {
        return "AdjustedFont{" +
                "mBase=" + mBase +
                ", mScale=" + mScale +
                ", mShiftX=" + mShiftX +
                ", mShiftY=" + mShiftY +
                '}';
    }
}
