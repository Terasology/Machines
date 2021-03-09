// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.ui;

import org.joml.Vector2i;
import org.terasology.engine.rendering.assets.texture.TextureRegion;
import org.terasology.engine.utilities.Assets;
import org.terasology.joml.geom.Rectanglei;
import org.terasology.math.TeraMath;
import org.terasology.nui.Canvas;
import org.terasology.nui.CoreWidget;
import org.terasology.nui.LayoutConfig;
import org.terasology.nui.ScaleMode;
import org.terasology.nui.databinding.Binding;
import org.terasology.nui.databinding.DefaultBinding;

public class HorizontalProgressBar extends CoreWidget {
    @LayoutConfig
    private TextureRegion fillTexture = Assets.getTexture("horizontalProgressBar").get();
    private Binding<Float> value = new DefaultBinding<>();

    @Override
    public void onDraw(Canvas canvas) {
        if (fillTexture != null) {
            float result = (float) TeraMath.clamp(getValue());

            Vector2i size = canvas.size();
            int drawWidth = Math.round(result * fillTexture.getWidth());
            canvas.drawTextureRaw(fillTexture, new Rectanglei(0, 0).setSize(drawWidth, size.y), ScaleMode.STRETCH,
                    0f, 0f, result, 1f);
        }
    }

    @Override
    public Vector2i getPreferredContentSize(Canvas canvas, Vector2i sizeHint) {
        if (fillTexture != null) {
            return fillTexture.size();
        }
        return new Vector2i();
    }

    public TextureRegion getFillTexture() {
        return fillTexture;
    }

    public void setFillTexture(TextureRegion fillTexture) {
        this.fillTexture = fillTexture;
    }

    public float getValue() {
        return value.get();
    }

    public void setValue(float value) {
        this.value.set(value);
    }

    public void bindValue(Binding<Float> binding) {
        this.value = binding;
    }
}
