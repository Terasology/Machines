// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.ui;

import org.joml.Vector2i;
import org.terasology.engine.math.JomlUtil;
import org.terasology.engine.rendering.assets.texture.TextureRegion;
import org.terasology.engine.utilities.Assets;
import org.terasology.math.TeraMath;
import org.terasology.nui.Canvas;
import org.terasology.nui.CoreWidget;
import org.terasology.nui.LayoutConfig;
import org.terasology.nui.ScaleMode;
import org.terasology.nui.databinding.Binding;
import org.terasology.nui.databinding.DefaultBinding;

public class VerticalProgressBar extends CoreWidget {
    @LayoutConfig
    private TextureRegion fillTexture = Assets.getTexture("engine:statusBar").get();
    private Binding<Float> value = new DefaultBinding<>();

    @Override
    public void onDraw(Canvas canvas) {
        if (fillTexture != null && getValue() != null) {
            float result = TeraMath.clamp(getValue());

            Vector2i size = canvas.size();
            int drawHeight = Math.round(result * fillTexture.getHeight());
            int offsetHeight = Math.round((1 - result) * fillTexture.getHeight());
            canvas.drawTextureRaw(fillTexture, JomlUtil.rectangleiFromMinAndSize(0, offsetHeight, size.x, drawHeight)
                    , ScaleMode.STRETCH,
                    0f, 0f, 1f, result);
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

    public Float getValue() {
        return value.get();
    }

    public void setValue(Float value) {
        this.value.set(value);
    }

    public void bindValue(Binding<Float> binding) {
        this.value = binding;
    }
}
