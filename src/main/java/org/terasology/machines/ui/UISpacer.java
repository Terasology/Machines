// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.ui;


import org.joml.Vector2i;
import org.terasology.nui.Canvas;
import org.terasology.nui.CoreWidget;
import org.terasology.nui.LayoutConfig;

public class UISpacer extends CoreWidget {

    @LayoutConfig
    private int width;
    @LayoutConfig
    private int height;

    @Override
    public void onDraw(Canvas canvas) {
    }

    @Override
    public Vector2i getPreferredContentSize(Canvas canvas, Vector2i sizeHint) {
        return new Vector2i(width, height);
    }
}
