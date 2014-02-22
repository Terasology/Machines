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

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.machines.components.MachineDefinitionComponent;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.nui.CoreScreenLayer;
import org.terasology.rendering.nui.NUIManager;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryGrid;
import org.terasology.rendering.nui.widgets.UIImage;
import org.terasology.workstation.ui.WorkstationUI;

public class DefaultMachineWindow extends CoreScreenLayer implements WorkstationUI {
    private InventoryGrid ingredients;
    private InventoryGrid tools;
    private InventoryGrid result;
    private InventoryGrid player;
    private UIImage stationBackground;

    private EntityRef station;

    @Override
    public void initialise() {
        ingredients = find("ingredientsInventory", InventoryGrid.class);
        tools = find("toolsInventory", InventoryGrid.class);
        result = find("resultInventory", InventoryGrid.class);
        player = find("playerInventory", InventoryGrid.class);
        stationBackground = find("stationBackground", UIImage.class);

    }

    @Override
    public void initializeWorkstation(final EntityRef entity) {
        this.station = entity;

        MachineDefinitionComponent machineDefinition = station.getComponent(MachineDefinitionComponent.class);
        int requirementInputSlots = machineDefinition.requirementSlots;
        int blockInputSlots = machineDefinition.inputSlots;
        int blockOutputSlots = machineDefinition.outputSlots;

        if( ingredients != null) {
            ingredients.setTargetEntity(station);
            ingredients.setCellOffset(0);
            ingredients.setMaxCellCount(blockInputSlots);
        }

        if( tools != null ) {
            tools.setTargetEntity(station);
            tools.setCellOffset(blockInputSlots);
            tools.setMaxCellCount(requirementInputSlots);
        }

        if( result != null) {
            result.setTargetEntity(station);
            result.setCellOffset(requirementInputSlots + blockInputSlots);
            result.setMaxCellCount(blockOutputSlots);
        }

        if( player != null) {
            player.setTargetEntity(CoreRegistry.get(LocalPlayer.class).getCharacterEntity());
            player.setCellOffset(10);
            player.setMaxCellCount(30);
        }

    }

    @Override
    public void update(float delta) {
        if (!station.exists()) {
            CoreRegistry.get(NUIManager.class).closeScreen(this);
            return;
        }
        super.update(delta);
    }

    @Override
    public boolean isModal() {
        return false;
    }
}
