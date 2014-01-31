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
package org.terasology.machines.gui;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Color;
import org.terasology.asset.Assets;
import org.terasology.engine.Time;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.machines.ProcessingManager;
import org.terasology.machines.components.DelayedProcessOutputComponent;
import org.terasology.machines.components.MachineDefinitionComponent;
import org.terasology.machines.components.ProcessingMachineComponent;
import org.terasology.machines.events.RequestProcessingEvent;
import org.terasology.machines.processParts.ProcessDescriptor;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.gui.framework.UIDisplayElement;
import org.terasology.rendering.gui.framework.events.ClickListener;
import org.terasology.rendering.gui.widgets.UIButton;
import org.terasology.rendering.gui.widgets.UIInventoryGrid;
import org.terasology.rendering.gui.widgets.UILabel;
import org.terasology.rendering.gui.widgets.UIProgressBar;
import org.terasology.rendering.gui.widgets.UITransferSlotCursor;
import org.terasology.rendering.gui.widgets.UIWindow;

import javax.vecmath.Vector2f;

public class UIScreenGenericProcessing extends UIWindow {
    public static final String UIGENERICPROCESSINGID = "UIGENERICPROCESSINGID";

    protected EntityRef player;
    protected EntityRef machineEntity;

    Time time;

    private final UILabel titleLabel;
    private final UIInventoryGrid inventory;
    private final UIInventoryGrid input;
    private final UIInventoryGrid output;
    private final UILabel inputTitle;
    private final UIInventoryGrid requirementsInput;
    private final UILabel requirementsInputTitle;
    private final UIButton goButton;
    private final UILabel outputLabel;
    private final UIProgressBar progressBar;


    public UIScreenGenericProcessing() {
        time = CoreRegistry.get(Time.class);
        setId(UIGENERICPROCESSINGID);
        setBackgroundColor(new Color(0, 0, 0, 200));
        setModal(true);
        setCloseKeys(new int[]{Keyboard.KEY_ESCAPE});
        maximize();

        titleLabel = new UILabel();
        titleLabel.setVisible(true);
        titleLabel.setPosition(new Vector2f(400, 130));
        titleLabel.setFont(Assets.getFont("engine:title"));


        inventory = new UIInventoryGrid(10);
        inventory.setVisible(true);
        inventory.layout();
        inventory.setPosition(new Vector2f(400, 400));

        output = new UIInventoryGrid(1);
        output.setVisible(true);
        output.setPosition(new Vector2f(600, 350));

        input = new UIInventoryGrid(1);
        input.setVisible(true);
        input.setPosition(new Vector2f(400, 200));

        inputTitle = new UILabel();
        inputTitle.setVisible(false);
        inputTitle.setPosition(new Vector2f(400, 170));
        inputTitle.setText("Material");

        requirementsInput = new UIInventoryGrid(1);
        requirementsInput.setVisible(true);
        requirementsInput.setPosition(new Vector2f(480, 200));

        requirementsInputTitle = new UILabel();
        requirementsInputTitle.setVisible(false);
        requirementsInputTitle.setPosition(new Vector2f(480, 170));
        requirementsInputTitle.setText("Tool");


        goButton = new UIButton(new Vector2f(100, 20), UIButton.ButtonType.NORMAL);
        goButton.getLabel().setText("Go");
        goButton.setVisible(true);
        goButton.setPosition(new Vector2f(600, 200));

        goButton.addClickListener(new ClickListener() {
            @Override
            public void click(UIDisplayElement element, int button) {
                machineEntity.send(new RequestProcessingEvent());
            }
        });

        outputLabel = new UILabel();
        outputLabel.setVisible(true);
        outputLabel.setPosition(new Vector2f(600, 220));

        progressBar = new UIProgressBar();
        progressBar.setVisible(false);
        progressBar.setPosition(new Vector2f(600, 300));
        progressBar.setSize(new Vector2f(100, 15));
        progressBar.setMax(100);

        addDisplayElement(output);
        addDisplayElement(titleLabel);
        addDisplayElement(inventory);
        addDisplayElement(input);
        addDisplayElement(inputTitle);
        addDisplayElement(requirementsInput);
        addDisplayElement(requirementsInputTitle);
        addDisplayElement(goButton);
        addDisplayElement(outputLabel);
        addDisplayElement(progressBar);
        addDisplayElement(new UITransferSlotCursor());
    }

    @Override
    public void open() {
        super.open();
        player = CoreRegistry.get(LocalPlayer.class).getCharacterEntity();
        inventory.linkToEntity(player, 0);
        layout();
    }

    @Override
    public void update() {
        if (isVisible()) {
            ProcessingMachineComponent processingMachineComponent = machineEntity.getComponent(ProcessingMachineComponent.class);
            DelayedProcessOutputComponent delayedProcessOutput = processingMachineComponent.outputEntity.getComponent(DelayedProcessOutputComponent.class);
            if (delayedProcessOutput != null) {
                float percentage = delayedProcessOutput.getPercentage(time.getGameTimeInMs());
                progressBar.setVisible(true);
                int progressValue = (int) (percentage * 100f);
                progressBar.setText(progressValue + "%");
                progressBar.setValue(progressValue);
            } else {
                progressBar.setVisible(false);
            }
        }

        super.update();
    }

    public void linkMachine(EntityRef entityRef) {

        machineEntity = entityRef;

        // set the machine title
        titleLabel.setText(ExtendedInventoryManager.getLabelFor(entityRef));

        ProcessingMachineComponent processingMachine = entityRef.getComponent(ProcessingMachineComponent.class);
        MachineDefinitionComponent machineDefinition = entityRef.getComponent(MachineDefinitionComponent.class);
        goButton.setVisible(!processingMachine.automaticProcessing);
        input.linkToEntity(processingMachine.inputEntity, 0, machineDefinition.blockInputSlots);
        inputTitle.setVisible(machineDefinition.blockInputSlots > 0);
        requirementsInput.linkToEntity(processingMachine.inputEntity, machineDefinition.blockInputSlots, machineDefinition.requirementInputSlots);
        requirementsInputTitle.setVisible(machineDefinition.requirementInputSlots > 0);
        output.linkToEntity(processingMachine.outputEntity, 0, machineDefinition.blockOutputSlots);

        refreshOutput();
    }

    public void refreshOutput() {
        ProcessingManager processingManager = CoreRegistry.get(ProcessingManager.class);
        ProcessingMachineComponent processingMachine = machineEntity.getComponent(ProcessingMachineComponent.class);
        Prefab outputPrefab = processingManager.getProcessDefinition(processingMachine.inputEntity, processingMachine.outputEntity);
        String outputText = "";
        if (outputPrefab != null) {
            for (Component component : outputPrefab.iterateComponents()) {
                if (component instanceof ProcessDescriptor) {
                    outputText += ((ProcessDescriptor) component).getDescription();
                }
            }
        }
        outputLabel.setText(outputText);
    }
}
