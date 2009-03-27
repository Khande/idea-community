/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModel;

/**
 * @author nik
 */
public class DefaultModulesProvider implements ModulesProvider {
  private final Project myProject;

  public DefaultModulesProvider(final Project project) {
    myProject = project;
  }

  public Module[] getModules() {
    return ModuleManager.getInstance(myProject).getModules();
  }

  public Module getModule(String name) {
    return ModuleManager.getInstance(myProject).findModuleByName(name);
  }

  public ModuleRootModel getRootModel(Module module) {
    return ModuleRootManager.getInstance(module);
  }
}