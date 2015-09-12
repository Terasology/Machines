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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.terasology.asset.Assets;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.inGameHelp.InGameHelpClient;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.machines.components.MachineDefinitionComponent;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.nui.BaseInteractionScreen;
import org.terasology.rendering.nui.UIWidget;
import org.terasology.rendering.nui.asset.UIElement;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryGrid;
import org.terasology.rendering.nui.layouts.ColumnLayout;
import org.terasology.rendering.nui.layouts.FlowLayout;
import org.terasology.rendering.nui.widgets.ActivateEventListener;
import org.terasology.rendering.nui.widgets.UIButton;
import org.terasology.rendering.nui.widgets.UIImage;
import org.terasology.rendering.nui.widgets.UILabel;
import org.terasology.workstation.component.WorkstationComponent;
import org.terasology.workstation.component.WorkstationProcessingComponent;
import org.terasology.workstation.event.WorkstationProcessRequest;
import org.terasology.workstation.process.DescribeProcess;
import org.terasology.workstation.process.ProcessPartDescription;
import org.terasology.workstation.process.ValidateProcess;
import org.terasology.workstation.process.WorkstationProcess;
import org.terasology.workstation.system.WorkstationRegistry;
import org.terasology.workstation.ui.ProcessListWidget;
import org.terasology.workstation.ui.WorkstationUI;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class DefaultMachineWindow extends BaseInteractionScreen {

    private InventoryGrid ingredients;
    private InventoryGrid tools;
    private InventoryGrid result;
    private UILabel ingredientsLabel;
    private UILabel toolsLabel;
    private UILabel resultLabel;
    private InventoryGrid player;
    private UIImage stationBackground;
    private HorizontalProgressBar progressBar;
    private UIButton executeButton;
    private UIContainer processResult;
    private UIContainer outputWidgets;
    private UIContainer inputWidgets;
    private ProcessListWidget processList;
    private UIContainer outputItems;

    private String validProcessId;
    private long nextProcessResultRefreshTime;

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
        executeButton = find("executeButton", UIButton.class);
        if (executeButton != null) {
            executeButton.subscribe(new ActivateEventListener() {
                @Override
                public void onActivated(UIWidget button) {
                    requestProcessExecution();
                }
            });
        }
        processResult = find("processResult", UIContainer.class);
        processList = find("processList", ProcessListWidget.class);
        outputWidgets = find("outputWidgets", UIContainer.class);
        inputWidgets = find("inputWidgets", UIContainer.class);
        outputItems = find("outputItems", UIContainer.class);
    }

    private void requestProcessExecution() {
        if (validProcessId != null) {
            CoreRegistry.get(LocalPlayer.class).getCharacterEntity().send(new WorkstationProcessRequest(getInteractionTarget(), validProcessId));
        }
    }

    @Override
    protected void initializeWithInteractionTarget(EntityRef interactionTarget) {
        WorkstationComponent workstation = getInteractionTarget().getComponent(WorkstationComponent.class);
        MachineDefinitionComponent machineDefinition = getInteractionTarget().getComponent(MachineDefinitionComponent.class);
        int requirementInputSlots = machineDefinition.requirementSlots;
        int blockInputSlots = machineDefinition.inputSlots;
        int blockOutputSlots = machineDefinition.outputSlots;

        if (inputWidgets != null) {
            if (machineDefinition.inputWidgets.isEmpty()) {
                inputWidgets.setContent(null);
                inputWidgets.setVisible(false);
            } else {
                inputWidgets.setVisible(true);
                FlowLayout widgetLayout = new FlowLayout();
                for (String widgetUri : machineDefinition.inputWidgets) {
                    UIElement widgetUIElement = Assets.getUIElement(widgetUri).get();
                    UIWidget widget = widgetUIElement.getRootWidget();

                    if (widget instanceof WorkstationUI) {
                        ((WorkstationUI) widget).initializeWorkstation(interactionTarget);
                    }
                    widgetLayout.addWidget(widget, null);
                }
                inputWidgets.setContent(widgetLayout);
            }
        }

        if (outputWidgets != null) {
            if (machineDefinition.outputWidgets.isEmpty()) {
                outputWidgets.setContent(null);
                outputWidgets.setVisible(false);
            } else {
                outputWidgets.setVisible(true);
                FlowLayout widgetLayout = new FlowLayout();
                for (String widgetUri : machineDefinition.outputWidgets) {
                    UIElement widgetUIElement = Assets.getUIElement(widgetUri).get();
                    UIWidget widget = widgetUIElement.getRootWidget();

                    if (widget instanceof WorkstationUI) {
                        ((WorkstationUI) widget).initializeWorkstation(interactionTarget);
                    }
                    widgetLayout.addWidget(widget, null);
                }
                outputWidgets.setContent(widgetLayout);
            }
        }

        if (ingredients != null) {
            ingredients.setTargetEntity(interactionTarget);
            ingredients.setCellOffset(0);
            ingredients.setMaxCellCount(blockInputSlots);
            ingredientsLabel.setVisible(blockInputSlots > 0);
            ingredientsLabel.setText(machineDefinition.inputSlotsTitle);
        }

        if (tools != null) {
            tools.setTargetEntity(interactionTarget);
            tools.setCellOffset(blockInputSlots);
            tools.setMaxCellCount(requirementInputSlots);
            toolsLabel.setVisible(requirementInputSlots > 0);
            toolsLabel.setText(machineDefinition.requirementSlotsTitle);
        }

        if (result != null) {
            result.setTargetEntity(interactionTarget);
            result.setCellOffset(requirementInputSlots + blockInputSlots);
            result.setMaxCellCount(blockOutputSlots);
            resultLabel.setVisible(blockOutputSlots > 0);
            resultLabel.setText(machineDefinition.outputSlotsTitle);
        }

        if (player != null) {
            player.setTargetEntity(CoreRegistry.get(LocalPlayer.class).getCharacterEntity());
            player.setCellOffset(10);
            player.setMaxCellCount(30);
        }

        if (progressBar != null) {
            progressBar.setVisible(false);
        }

        if (executeButton != null) {
            // hide the button, if there arent any manual processes
            executeButton.setVisible(workstation.supportedProcessTypes.values().contains(false));
            executeButton.setText(machineDefinition.actionTitle);
        }

        if (processList != null) {
            processList.initializeWorkstation(interactionTarget);
        }

        if (outputItems != null) {
            ColumnLayout itemsLayout = new ColumnLayout();
            itemsLayout.setColumns(5);
            itemsLayout.setAutoSizeColumns(true);
            itemsLayout.setFillVerticalSpace(false);

            WorkstationRegistry workstationRegistry = CoreRegistry.get(WorkstationRegistry.class);

            Set<String> alreadyAddedResourceUrns = Sets.newHashSet();
            for (WorkstationProcess process : workstationRegistry.getWorkstationProcesses(workstation.supportedProcessTypes.keySet())) {
                if (process instanceof DescribeProcess) {
                    DescribeProcess describeProcess = (DescribeProcess) process;
                    List<ProcessPartDescription> processPartDescriptions = Lists.newArrayList(describeProcess.getOutputDescriptions());
                    // sort the processDescriptions so that visual order is the same all the time
                    Collections.sort(processPartDescriptions, new Comparator<ProcessPartDescription>() {
                        @Override
                        public int compare(ProcessPartDescription o1, ProcessPartDescription o2) {
                            return o1.getResourceUrn().compareTo(o2.getResourceUrn());
                        }
                    });
                    for (ProcessPartDescription processPartDescription : processPartDescriptions) {
                        final String hyperlink = processPartDescription.getResourceUrn() != null ? processPartDescription.getResourceUrn().toString() : null;
                        if (hyperlink == null || !alreadyAddedResourceUrns.contains(hyperlink)) {
                            if (hyperlink != null) {
                                alreadyAddedResourceUrns.add(hyperlink);
                            }
                            OverlapLayout overlapLayout = new OverlapLayout();
                            overlapLayout.addWidget(processPartDescription.getWidget());
                            overlapLayout.subscribe(x -> CoreRegistry.get(InGameHelpClient.class).showHelpForHyperlink(hyperlink));
                            itemsLayout.addWidget(overlapLayout);
                        }
                    }
                }
            }
            outputItems.setContent(itemsLayout);
        }
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        if (getInteractionTarget() == null) {
            return;
        } else {
            if (progressBar != null) {
                // update the progress bar
                WorkstationProcessingComponent processing = getInteractionTarget().getComponent(WorkstationProcessingComponent.class);
                if (processing != null && processing.processes.size() > 0) {
                    for (WorkstationProcessingComponent.ProcessDef processDef : processing.processes.values()) {
                        Time time = CoreRegistry.get(Time.class);
                        long currentTime = time.getGameTimeInMs();
                        float value = 1.0f - (float) (processDef.processingFinishTime - currentTime)
                                / (float) (processDef.processingFinishTime - processDef.processingStartTime);
                        progressBar.setValue(Math.max(value, 0f));
                        progressBar.setVisible(true);
                    }
                } else {
                    progressBar.setVisible(false);
                }
            }

            Time time = CoreRegistry.get(Time.class);
            long currentTime = time.getRealTimeInMs();

            // find the process that will run
            if (processResult != null && currentTime > nextProcessResultRefreshTime) {
                nextProcessResultRefreshTime = currentTime + 200;
                // check for valid processes
                EntityRef character = CoreRegistry.get(LocalPlayer.class).getCharacterEntity();
                WorkstationRegistry workstationRegistry = CoreRegistry.get(WorkstationRegistry.class);
                WorkstationComponent workstation = getInteractionTarget().getComponent(WorkstationComponent.class);

                // isolate the valid processes to one single process
                WorkstationProcess mostComplexProcess = null;
                for (WorkstationProcess process : workstationRegistry.getWorkstationProcesses(workstation.supportedProcessTypes.keySet())) {
                    if (process instanceof ValidateProcess) {
                        ValidateProcess validateProcess = (ValidateProcess) process;
                        if (validateProcess.isValid(character, getInteractionTarget())) {
                            if (process instanceof DescribeProcess) {
                                if (mostComplexProcess == null || getComplexity((DescribeProcess) process) > getComplexity((DescribeProcess) mostComplexProcess)) {
                                    mostComplexProcess = process;
                                }
                            }
                        }
                    }
                }
                if (mostComplexProcess != null) {
                    validProcessId = mostComplexProcess.getId();
                    if (mostComplexProcess instanceof DescribeProcess) {
                        FlowLayout flowLayout = new FlowLayout();
                        for (ProcessPartDescription processPartDescription : ((DescribeProcess) mostComplexProcess).getOutputDescriptions()) {
                            flowLayout.addWidget(processPartDescription.getWidget(), null);
                        }
                        processResult.setContent(flowLayout);
                    } else {
                        UILabel processIdLabel = new UILabel();
                        processIdLabel.setText(validProcessId);
                        processResult.setContent(processIdLabel);
                    }
                    processResult.setVisible(true);
                    executeButton.setVisible(true && workstation.supportedProcessTypes.values().contains(false));
                } else {
                    validProcessId = null;
                    processResult.setContent(null);
                    processResult.setVisible(false);
                    executeButton.setVisible(false);
                }
            }
        }
    }

    private int getComplexity(DescribeProcess process) {
        return Iterables.size(Iterables.filter(process.getInputDescriptions(), x -> x.getResourceUrn() != null));
    }

    @Override
    public boolean isModal() {
        return false;
    }
}
