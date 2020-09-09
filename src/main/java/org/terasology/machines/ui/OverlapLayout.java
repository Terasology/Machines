// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.ui;

import com.google.common.collect.Lists;
import org.joml.Vector2i;
import org.terasology.nui.BaseInteractionListener;
import org.terasology.nui.Canvas;
import org.terasology.nui.CoreLayout;
import org.terasology.nui.InteractionListener;
import org.terasology.nui.LayoutHint;
import org.terasology.nui.UIWidget;
import org.terasology.nui.events.NUIMouseClickEvent;
import org.terasology.nui.input.MouseInput;
import org.terasology.nui.widgets.ActivateEventListener;

import java.util.Iterator;
import java.util.List;

public class OverlapLayout extends CoreLayout<LayoutHint> {
    private final List<UIWidget> widgets = Lists.newLinkedList();
    private final List<ActivateEventListener> listeners = Lists.newArrayList();
    private final InteractionListener interactionListener = new BaseInteractionListener() {

        @Override
        public boolean onMouseClick(NUIMouseClickEvent event) {
            if (event.getMouseButton() == MouseInput.MOUSE_LEFT) {
                activate();
            }
            return false;
        }
    };
    private boolean down;

    public OverlapLayout() {
    }

    public OverlapLayout(String id) {
        super(id);
    }

    public void addWidget(UIWidget widget) {
        widgets.add(widget);
    }

    @Override
    public void addWidget(UIWidget element, LayoutHint hint) {
        addWidget(element);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.addInteractionRegion(interactionListener);
        for (UIWidget uiWidget : widgets) {
            if (uiWidget != null) {
                canvas.drawWidget(uiWidget);
            }
        }
    }

    @Override
    public Vector2i getPreferredContentSize(Canvas canvas, Vector2i sizeHint) {
        int maxX = 0;
        int maxY = 0;
        for (UIWidget uiWidget : widgets) {
            Vector2i preferredContentSize = uiWidget.getPreferredContentSize(canvas, sizeHint);
            maxX = Math.max(maxX, preferredContentSize.x);
            maxY = Math.max(maxY, preferredContentSize.y);
        }

        return new Vector2i(maxX, maxY);
    }

    @Override
    public Vector2i getMaxContentSize(Canvas canvas) {
        int maxX = 0;
        int maxY = 0;
        for (UIWidget uiWidget : widgets) {
            Vector2i maxContentSize = uiWidget.getMaxContentSize(canvas);
            maxX = Math.max(maxX, maxContentSize.x);
            maxY = Math.max(maxY, maxContentSize.y);
        }

        return new Vector2i(maxX, maxY);
    }

    @Override
    public Iterator<UIWidget> iterator() {
        return widgets.iterator();
    }

    private void activate() {
        for (ActivateEventListener listener : listeners) {
            listener.onActivated(this);
        }
    }

    public void subscribe(ActivateEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(ActivateEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void removeWidget(UIWidget element) {
        widgets.remove(element);
    }
}
