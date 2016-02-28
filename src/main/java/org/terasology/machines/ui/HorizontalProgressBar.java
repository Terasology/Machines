/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.machines.ui;

import org.terasology.utilities.Assets;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.TeraMath;
import org.terasology.math.geom.Vector2i;
import org.terasology.rendering.assets.texture.TextureRegion;
import org.terasology.rendering.nui.Canvas;
import org.terasology.rendering.nui.CoreWidget;
import org.terasology.rendering.nui.LayoutConfig;
import org.terasology.rendering.nui.ScaleMode;
import org.terasology.rendering.nui.databinding.Binding;
import org.terasology.rendering.nui.databinding.DefaultBinding;

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
            canvas.drawTextureRaw(fillTexture, Rect2i.createFromMinAndSize(0, 0, drawWidth, size.y), ScaleMode.STRETCH,
                    0f, 0f, result, 1f);
        }
    }

    @Override
    public Vector2i getPreferredContentSize(Canvas canvas, Vector2i sizeHint) {
        if (fillTexture != null) {
            return fillTexture.size();
        }
        return Vector2i.zero();
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
