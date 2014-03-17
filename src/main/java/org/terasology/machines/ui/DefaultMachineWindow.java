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

import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.machines.components.MachineDefinitionComponent;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.nui.CoreScreenLayer;
import org.terasology.rendering.nui.NUIManager;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryGrid;
import org.terasology.rendering.nui.widgets.UIImage;
import org.terasology.rendering.nui.widgets.UILabel;
import org.terasology.workstation.component.WorkstationProcessingComponent;
import org.terasology.workstation.ui.WorkstationUI;

public class DefaultMachineWindow extends CoreScreenLayer implements WorkstationUI {

    protected EntityRef station;

    private InventoryGrid ingredients;
    private InventoryGrid tools;
    private InventoryGrid result;
    private UILabel ingredientsLabel;
    private UILabel toolsLabel;
    private UILabel resultLabel;
    private InventoryGrid player;
    private UIImage stationBackground;
    private HorizontalProgressBar progressBar;

    @Override
    public void initialise() {
        ingredients = find("ingredientsInventory", InventoryGrid.class);
        tools = find("toolsInventory", InventoryGrid.class);
        result = find("resultInventory", InventoryGrid.class);
        ingredientsLabel = find("ingredientsInventoryLabel", UILabel.class);
        toolsLabel = find("toolsInventoryLabel", UILabel.class);
        resultLabel = find("resultInventoryLabel", UILabel.class);
        player = find("playerInventory", InventoryGrid.class);
        stationBackground = find("stationBackground", UIImage.class);
        progressBar = find("progressBar", HorizontalProgressBar.class);

    }

    @Override
    public void initializeWorkstation(final EntityRef entity) {
        this.station = entity;

        MachineDefinitionComponent machineDefinition = station.getComponent(MachineDefinitionComponent.class);
        int requirementInputSlots = machineDefinition.requirementSlots;
        int blockInputSlots = machineDefinition.inputSlots;
        int blockOutputSlots = machineDefinition.outputSlots;

        if (ingredients != null) {
            ingredients.setTargetEntity(station);
            ingredients.setCellOffset(0);
            ingredients.setMaxCellCount(blockInputSlots);
            ingredientsLabel.setVisible(blockInputSlots > 0);
        }

        if (tools != null) {
            tools.setTargetEntity(station);
            tools.setCellOffset(blockInputSlots);
            tools.setMaxCellCount(requirementInputSlots);
            toolsLabel.setVisible(requirementInputSlots > 0);
        }

        if (result != null) {
            result.setTargetEntity(station);
            result.setCellOffset(requirementInputSlots + blockInputSlots);
            result.setMaxCellCount(blockOutputSlots);
            resultLabel.setVisible(blockOutputSlots > 0);
        }

        if (player != null) {
            player.setTargetEntity(CoreRegistry.get(LocalPlayer.class).getCharacterEntity());
            player.setCellOffset(10);
            player.setMaxCellCount(30);
        }

        progressBar.setVisible(false);

    }

    @Override
    public void update(float delta) {
        if (!station.exists()) {
            CoreRegistry.get(NUIManager.class).closeScreen(this);
            return;
        } else {
            WorkstationProcessingComponent processing = station.getComponent(WorkstationProcessingComponent.class);
            if (processing != null && processing.processes.size() > 0) {
                for (WorkstationProcessingComponent.ProcessDef processDef : processing.processes.values()) {
                    Time time = CoreRegistry.get(Time.class);
                    long currentTime = time.getGameTimeInMs();
                    float value = 1.0f - (float) (processDef.processingFinishTime - currentTime) / (float) (processDef.processingFinishTime - processDef.processingStartTime);
                    progressBar.setValue(Math.max(value, 0f));
                    progressBar.setVisible(true);
                }
            } else {
                progressBar.setVisible(false);
            }
        }
        super.update(delta);
    }

    @Override
    public boolean isModal() {
        return false;
    }
}

