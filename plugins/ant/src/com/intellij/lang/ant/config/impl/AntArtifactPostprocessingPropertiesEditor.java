package com.intellij.lang.ant.config.impl;

import com.intellij.lang.ant.config.AntBuildTarget;
import com.intellij.lang.ant.config.AntConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FixedSizeButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.ui.ArtifactPropertiesEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author nik
 */
public class AntArtifactPostprocessingPropertiesEditor extends ArtifactPropertiesEditor {
  private final AntArtifactPostprocessingProperties myProperties;
  private final Project myProject;
  private JPanel myMainPanel;
  private JCheckBox myRunTargetCheckBox;
  private FixedSizeButton mySelectTargetButton;
  private AntBuildTarget myTarget;

  public AntArtifactPostprocessingPropertiesEditor(AntArtifactPostprocessingProperties properties, Project project) {
    myProperties = properties;
    myProject = project;
    mySelectTargetButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectTarget();
      }
    });
    myRunTargetCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mySelectTargetButton.setEnabled(myRunTargetCheckBox.isSelected());
        if (myRunTargetCheckBox.isSelected() && myTarget == null) {
          selectTarget();
        }
      }
    });
  }

  private void selectTarget() {
    final TargetChooserDialog dialog = new TargetChooserDialog(myProject, myTarget, AntConfiguration.getInstance(myProject));
    dialog.show();
    if (dialog.isOK()) {
      myTarget = dialog.getSelectedTarget();
      updateLabel();
    }
  }

  private void updateLabel() {
    if (myTarget != null) {
      myRunTargetCheckBox.setText("Run Ant target '" + myTarget.getName() + "'");
    }
    else {
      myRunTargetCheckBox.setText("Run Ant target <none>");
    }
  }

  public String getTabName() {
    return POSTPROCESSING_TAB;
  }

  public void apply() {
    myProperties.setEnabled(myRunTargetCheckBox.isSelected());
    if (myTarget != null) {
      final VirtualFile file = myTarget.getModel().getBuildFile().getVirtualFile();
      if (file != null) {
        myProperties.setFileUrl(file.getUrl());
        myProperties.setTargetName(myTarget.getName());
        return;
      }
    }
    myProperties.setFileUrl(null);
    myProperties.setTargetName(null);
  }

  public JComponent createComponent() {
    return myMainPanel;
  }

  public boolean isModified() {
    if (myProperties.isEnabled() != myRunTargetCheckBox.isSelected()) return true;
    if (myTarget == null) {
      return myProperties.getFileUrl() != null;
    }
    if (!Comparing.equal(myTarget.getName(), myProperties.getTargetName())) return true;
    final VirtualFile file = myTarget.getModel().getBuildFile().getVirtualFile();
    return file != null && !Comparing.equal(file.getUrl(), myProperties.getFileUrl());
  }

  public void reset() {
    myRunTargetCheckBox.setSelected(myProperties.isEnabled());
    myTarget = myProperties.findTarget(AntConfiguration.getInstance(myProject));
    updateLabel();
  }

  public void disposeUIResources() {
  }
}