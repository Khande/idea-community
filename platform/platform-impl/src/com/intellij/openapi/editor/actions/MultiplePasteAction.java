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
package com.intellij.openapi.editor.actions;

import com.intellij.ide.CopyPasteManagerEx;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbAware;
import com.intellij.ui.UIBundle;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author max
 */
public class MultiplePasteAction extends AnAction implements DumbAware {
  public MultiplePasteAction() {
    setEnabledInModalContext(true);
  }

  public void actionPerformed(final AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    Project project = PlatformDataKeys.PROJECT.getData(dataContext);
    Component focusedComponent = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);

    if (!(focusedComponent instanceof JComponent)) return;

    final ContentChooser<Transferable> chooser = new ContentChooser<Transferable>(project, UIBundle.message(
      "choose.content.to.paste.dialog.title"), true){
      protected String getStringRepresentationFor(final Transferable content) {
        try {
          return (String)content.getTransferData(DataFlavor.stringFlavor);
        }
        catch (UnsupportedFlavorException e1) {
          return "";
        }
        catch (IOException e1) {
          return "";
        }
      }

      protected List<Transferable> getContents() {
        return Arrays.asList(CopyPasteManager.getInstance().getAllContents());
      }

      protected void removeContentAt(final Transferable content) {
        ((CopyPasteManagerEx)CopyPasteManager.getInstance()).removeContent(content);
      }
    };

    if (chooser.getAllContents().size() > 0) {
      chooser.show();
    }
    else {
      chooser.close(ContentChooser.CANCEL_EXIT_CODE);
    }

    if (chooser.isOK()) {
      final int selectedIndex = chooser.getSelectedIndex();
      ((CopyPasteManagerEx)CopyPasteManager.getInstance()).moveContentTopStackTop(chooser.getAllContents().get(selectedIndex));

      if (editor != null) {
          if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)){
            return;
          }

        final AnAction pasteAction = ActionManager.getInstance().getAction(IdeActions.ACTION_PASTE);
        AnActionEvent newEvent = new AnActionEvent(e.getInputEvent(),
                                                   DataManager.getInstance().getDataContext(focusedComponent),
                                                   e.getPlace(), e.getPresentation(),
                                                   ActionManager.getInstance(),
                                                   e.getModifiers());
        pasteAction.actionPerformed(newEvent);
      }
      else {
        final Action pasteAction = ((JComponent)focusedComponent).getActionMap().get(DefaultEditorKit.pasteAction);
        if (pasteAction != null) {
          pasteAction.actionPerformed(new ActionEvent(focusedComponent, ActionEvent.ACTION_PERFORMED, ""));
        }
      }
    }
  }

  public void update(AnActionEvent e) {
    final boolean enabled = isEnabled(e);
    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      e.getPresentation().setVisible(enabled);
    }
    else {
      e.getPresentation().setEnabled(enabled);
    }
  }

  private static boolean isEnabled(AnActionEvent e) {
    Object component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    if (!(component instanceof JComponent)) return false;
    if (e.getData(PlatformDataKeys.EDITOR) != null) return true;
    Action pasteAction = ((JComponent)component).getActionMap().get(DefaultEditorKit.pasteAction);
    return pasteAction != null;
  }

}
