/*
 * Copyright 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.machines.systems;

import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.input.ButtonState;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.events.InventorySlotChangedEvent;
import org.terasology.logic.manager.GUIManager;
import org.terasology.machines.ProcessingManager;
import org.terasology.machines.components.ProcessRequirementsProviderComponent;
import org.terasology.machines.components.ProcessingActionComponent;
import org.terasology.machines.events.PlayerProcessingButton;
import org.terasology.machines.gui.UIScreenGenericProcessing;
import org.terasology.network.ClientComponent;
import org.terasology.registry.In;
import org.terasology.rendering.gui.widgets.UIWindow;

@RegisterSystem(RegisterMode.CLIENT)
public class ProcessingMachineClientSystem implements ComponentSystem {

    @In
    GUIManager guiManager;
    @In
    EntityManager entityManager;
    @In
    ProcessingManager processingManager;

    @Override
    public void initialise() {
        guiManager.registerWindow(UIScreenGenericProcessing.UIGENERICPROCESSINGID, UIScreenGenericProcessing.class);
    }

    @Override
    public void shutdown() {
    }

    @ReceiveEvent(components = CharacterComponent.class)
    public void onPlayerProcessingButton(PlayerProcessingButton event, EntityRef entity) {
        if (event.getState() == ButtonState.DOWN) {
            guiManager.toggleWindow(UIScreenGenericProcessing.UIGENERICPROCESSINGID);
            UIScreenGenericProcessing screen = (UIScreenGenericProcessing) guiManager.getWindowById(UIScreenGenericProcessing.UIGENERICPROCESSINGID);

            if (screen.isVisible()) {
                screen.linkMachine(entity);
            }
            event.consume();
        }
    }

    @ReceiveEvent
    public void onInventorySlotChangedEvent(InventorySlotChangedEvent event, EntityRef entity, ProcessRequirementsProviderComponent keyEntity) {
        // recalculate the assembly result for display
        UIWindow focusedWindow = guiManager.getFocusedWindow();
        if (UIScreenGenericProcessing.class.isAssignableFrom(focusedWindow.getClass())) {
            UIScreenGenericProcessing screen = (UIScreenGenericProcessing) focusedWindow;
            if (screen.isVisible()) {
                screen.refreshOutput();
            }
        }

    }


    @ReceiveEvent
    public void onActivate(ActivateEvent event, EntityRef entity, ProcessingActionComponent processingAction) {
        EntityRef instigator = event.getInstigator();
        ClientComponent controller = instigator.getComponent(CharacterComponent.class).controller.getComponent(ClientComponent.class);
        if (controller.local) {
            UIScreenGenericProcessing screen = (UIScreenGenericProcessing) guiManager.openWindow(processingAction.screenId);
            screen.linkMachine(entity);
            screen.open();
        }
    }


}
