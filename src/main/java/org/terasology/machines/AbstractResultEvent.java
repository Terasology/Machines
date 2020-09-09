// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines;

import org.terasology.engine.entitySystem.event.Event;
import org.terasology.engine.network.NoReplicate;

public abstract class AbstractResultEvent<T> implements Event {
    @NoReplicate
    protected T result;

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

}
