/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public abstract class DialogWrapperPeerFactory {
  public static DialogWrapperPeerFactory getInstance() {
    if (ApplicationManager.getApplication() == null) {
      try {
        return (DialogWrapperPeerFactory)Class.forName("com.intellij.openapi.ui.impl.DialogWrapperPeerFactoryImpl").newInstance();
      }
      catch (Exception e) {
        throw new RuntimeException("Can't instantiate DialogWrapperPeerFactory", e);
      }
    }

    return ServiceManager.getService(DialogWrapperPeerFactory.class);
  }

  public abstract DialogWrapperPeer createPeer(DialogWrapper wrapper, Project project, boolean canBeParent);
  public abstract DialogWrapperPeer createPeer(DialogWrapper wrapper, boolean canBeParent);
  public abstract DialogWrapperPeer createPeer(DialogWrapper wrapper, boolean canBeParent, boolean tryToolkitModal);
  public abstract DialogWrapperPeer createPeer(DialogWrapper wrapper, @NotNull Component parent, boolean canBeParent);
}