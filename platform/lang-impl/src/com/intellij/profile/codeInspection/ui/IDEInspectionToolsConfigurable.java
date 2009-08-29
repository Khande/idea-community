/*
 * User: anna
 * Date: 17-Apr-2009
 */
package com.intellij.profile.codeInspection.ui;

import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Comparing;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;

public class IDEInspectionToolsConfigurable extends InspectionToolsConfigurable {
  public IDEInspectionToolsConfigurable(InspectionProjectProfileManager projectProfileManager, InspectionProfileManager profileManager) {
    super(projectProfileManager, profileManager);
  }

  protected InspectionProfileImpl getCurrentProfile() {
    return (InspectionProfileImpl)((InspectionProfileManager)myProfileManager).getRootProfile();
  }

  @Override
  public void apply() throws ConfigurationException {
    super.apply();
    myProfileManager.setRootProfile(getSelectedObject().getName());
  }

  @Override
  public boolean isModified() {
    if (!Comparing.strEqual(getSelectedObject().getName(), getCurrentProfile().getName())) return true;
    return super.isModified();
  }
}