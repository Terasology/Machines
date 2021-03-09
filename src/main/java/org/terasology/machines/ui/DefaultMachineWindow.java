// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.machines.ui;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.registry.CoreRegistry;
import org.terasology.engine.rendering.nui.BaseInteractionScreen;
import org.terasology.engine.rendering.nui.layers.ingame.inventory.InventoryGrid;
import org.terasology.engine.utilities.Assets;
import org.terasology.inGameHelp.InGameHelpClient;
import org.terasology.machines.components.MachineDefinitionComponent;
import org.terasology.nui.UILayout;
import org.terasology.nui.UIWidget;
import org.terasology.nui.asset.UIElement;
import org.terasology.nui.layouts.ColumnLayout;
import org.terasology.nui.layouts.FlowLayout;
import org.terasology.nui.widgets.ActivateEventListener;
import org.terasology.nui.widgets.UIButton;
import org.terasology.nui.widgets.UIImage;
import org.terasology.nui.widgets.UILabel;
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

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

public class DefaultMachineWindow extends BaseInteractionScreen {
    private static final Logger logger = LoggerFactory.getLogger(DefaultMachineWindow.class);

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
        WorkstationComponent workstation = interactionTarget.getComponent(WorkstationComponent.class);
        MachineDefinitionComponent machineDefinition = interactionTarget.getComponent(MachineDefinitionComponent.class);
        int requirementInputSlots = machineDefinition.requirementSlots;
        int blockInputSlots = machineDefinition.inputSlots;
        int blockOutputSlots = machineDefinition.outputSlots;

        initializeIoWidgets(inputWidgets, interactionTarget, machineDefinition.inputWidgets);

        initializeIoWidgets(outputWidgets, interactionTarget, machineDefinition.outputWidgets);

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
            // hide the button, if there aren't any manual processes
            executeButton.setVisible(workstation.supportedProcessTypes.values().contains(false));
            executeButton.setText(machineDefinition.actionTitle);
        }

        if (processList != null) {
            processList.initializeWorkstation(interactionTarget);
        }

        if (outputItems != null) {
            outputItems.setContent(layoutOutputItems(workstation));
        }
    }

    private static UILayout layoutOutputItems(WorkstationComponent workstation) {
        WorkstationRegistry workstationRegistry = CoreRegistry.get(WorkstationRegistry.class);
        InGameHelpClient helpClient = CoreRegistry.get(InGameHelpClient.class);

        ColumnLayout itemsLayout = new ColumnLayout();
        itemsLayout.setColumns(5);
        itemsLayout.setAutoSizeColumns(true);
        itemsLayout.setFillVerticalSpace(false);

        Set<String> alreadyAddedResourceUrns = Sets.newHashSet();

        Collection<WorkstationProcess> processes =
                workstationRegistry.getWorkstationProcesses(workstation.supportedProcessTypes.keySet());

        processes.stream()
                .filter(process -> process instanceof DescribeProcess)
                .flatMap(process -> ((DescribeProcess) process).getOutputDescriptions().stream())
                .sorted(Comparator.comparing(ProcessPartDescription::getDisplayName))
                .forEach(processPartDescription -> {
                    final String hyperlink = processPartDescription.getResourceUrn() != null ?
                            processPartDescription.getResourceUrn().toString() : null;
                    if (hyperlink == null || !alreadyAddedResourceUrns.contains(hyperlink)) {
                        if (hyperlink != null) {
                            alreadyAddedResourceUrns.add(hyperlink);
                        }
                        OverlapLayout overlapLayout = new OverlapLayout();
                        overlapLayout.addWidget(processPartDescription.getWidget());
                        overlapLayout.subscribe(x -> helpClient.showHelpForHyperlink(hyperlink));
                        itemsLayout.addWidget(overlapLayout);
                    }
                });

        return itemsLayout;
    }

    private static void initializeIoWidgets(UIContainer widgetContainer, EntityRef interactionTarget,
                                            Set<String> machineWidgetUris) {
        if (widgetContainer != null) {
            if (machineWidgetUris.isEmpty()) {
                widgetContainer.setContent(null);
                widgetContainer.setVisible(false);
            } else {
                widgetContainer.setVisible(true);
                FlowLayout widgetLayout = new FlowLayout();
                for (String widgetUri : machineWidgetUris) {
                    UIElement widgetUIElement;
                    Optional<UIElement> uiElementOptional = Assets.getUIElement(widgetUri);
                    if (!uiElementOptional.isPresent()) {
                        continue;  // FIXME: log warnings here!
                    }
                    widgetUIElement = uiElementOptional.get();
                    UIWidget widget = widgetUIElement.getRootWidget();

                    if (widget instanceof WorkstationUI) {
                        ((WorkstationUI) widget).initializeWorkstation(interactionTarget);
                    }
                    widgetLayout.addWidget(widget, null);
                }
                widgetContainer.setContent(widgetLayout);
            }
        }
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        final EntityRef interactionTarget = getInteractionTarget();
        if (interactionTarget == null) {
            return;
        } else if (!interactionTarget.exists()) {
            logger.error("interaction target does not exist? {} {}", this, interactionTarget);
            return;
        }


        if (progressBar != null) {
            // update the progress bar
            updateProgressBar(progressBar, interactionTarget);
        }


        // find the process that will run
        if (processResult != null) {
            updateProcessResult(processResult, interactionTarget);
        }
    }

    private void updateProcessResult(UIContainer targetProcessResult, EntityRef interactionTarget) {
        Time time = CoreRegistry.get(Time.class);
        long currentTime = time.getGameTimeInMs();

        if (currentTime < nextProcessResultRefreshTime) {
            return;
        }
        nextProcessResultRefreshTime = currentTime + 200;

        // check for valid processes
        WorkstationComponent workstation = interactionTarget.getComponent(WorkstationComponent.class);
        WorkstationProcess mostComplexProcess = getMostComplexProcess(workstation, interactionTarget);

        UIWidget resultContent = null;
        boolean buttonVisible = false;

        if (mostComplexProcess != null) {
            validProcessId = mostComplexProcess.getId();
            if (mostComplexProcess instanceof DescribeProcess) {
                FlowLayout flowLayout = new FlowLayout();
                for (ProcessPartDescription processPartDescription :
                        ((DescribeProcess) mostComplexProcess).getOutputDescriptions()) {
                    flowLayout.addWidget(processPartDescription.getWidget(), null);
                }
                resultContent = flowLayout;
            } else {
                resultContent = new UILabel(validProcessId);
            }
            buttonVisible = workstation.supportedProcessTypes.values().contains(false);
        } else {
            validProcessId = null;
        }

        targetProcessResult.setContent(resultContent);
        targetProcessResult.setVisible(resultContent != null);
        if (executeButton != null) {
            executeButton.setVisible(buttonVisible);
        }
    }

    private static WorkstationProcess getMostComplexProcess(WorkstationComponent workstation,
                                                            EntityRef interactionTarget) {
        EntityRef character = CoreRegistry.get(LocalPlayer.class).getCharacterEntity();
        WorkstationRegistry workstationRegistry = CoreRegistry.get(WorkstationRegistry.class);
        final Collection<WorkstationProcess> processes =
                workstationRegistry.getWorkstationProcesses(workstation.supportedProcessTypes.keySet());
        // isolate the valid processes to one single process
        WorkstationProcess mostComplexProcess = null;
        for (WorkstationProcess process : processes) {
            if (process instanceof ValidateProcess) {
                ValidateProcess validateProcess = (ValidateProcess) process;
                if (validateProcess.isValid(character, interactionTarget)) {
                    if (process instanceof DescribeProcess) {
                        if (mostComplexProcess == null || getComplexity((DescribeProcess) process) > getComplexity((DescribeProcess) mostComplexProcess)) {
                            mostComplexProcess = process;
                        }
                    }
                }
            }
        }
        return mostComplexProcess;
    }

    private static void updateProgressBar(HorizontalProgressBar progressBar, EntityRef interactionTarget) {
        Time time = CoreRegistry.get(Time.class);
        long currentTime = time.getGameTimeInMs();

        WorkstationProcessingComponent processing =
                interactionTarget.getComponent(WorkstationProcessingComponent.class);
        if (processing != null && processing.processes.size() > 0) {
            for (WorkstationProcessingComponent.ProcessDef processDef : processing.processes.values()) {
                float value = 1.0f - (float) (processDef.processingFinishTime - currentTime)
                        / (float) (processDef.processingFinishTime - processDef.processingStartTime);
                progressBar.setValue(Math.max(value, 0f));
                progressBar.setVisible(true);
            }
        } else {
            progressBar.setVisible(false);
        }
    }

    private static int getComplexity(DescribeProcess process) {
        return process.getInputDescriptions().size();
    }

    @Override
    public boolean isModal() {
        return false;
    }
}
