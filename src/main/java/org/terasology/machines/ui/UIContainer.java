// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.ui;


import org.joml.Vector2i;
import org.terasology.nui.Canvas;
import org.terasology.nui.CoreWidget;
import org.terasology.nui.LayoutConfig;
import org.terasology.nui.UIWidget;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class UIContainer extends CoreWidget {

    @LayoutConfig
    private UIWidget content;

    @Override
    public void onDraw(Canvas canvas) {
        if (content != null) {
            canvas.drawWidget(content);
        }
    }

    @Override
    public Vector2i getPreferredContentSize(Canvas canvas, Vector2i sizeHint) {
        if (content != null) {
            return canvas.calculateRestrictedSize(content, sizeHint);
        }
        return new Vector2i();
    }

    public UIWidget getContent() {
        return content;
    }

    public void setContent(UIWidget content) {
        this.content = content;
    }

    @Override
    public Iterator<UIWidget> iterator() {
        if (content != null) {
            return Arrays.asList(content).iterator();
        }
        return Collections.emptyIterator();
    }
}
