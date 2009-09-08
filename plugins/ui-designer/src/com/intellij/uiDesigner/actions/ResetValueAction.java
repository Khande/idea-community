package com.intellij.uiDesigner.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.uiDesigner.radComponents.RadComponent;
import com.intellij.uiDesigner.designSurface.GuiEditor;
import com.intellij.uiDesigner.propertyInspector.Property;
import com.intellij.uiDesigner.propertyInspector.PropertyInspectorTable;
import com.intellij.uiDesigner.propertyInspector.UIDesignerToolWindowManager;
import com.intellij.uiDesigner.propertyInspector.PropertyInspector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class ResetValueAction extends AbstractGuiEditorAction {
  private static final Logger LOG = Logger.getInstance("#com.intellij.uiDesigner.actions.ResetValueAction");

  protected void actionPerformed(final GuiEditor editor, final List<RadComponent> selection, final AnActionEvent e) {
    final PropertyInspectorTable inspector = (PropertyInspectorTable)e.getDataContext().getData(PropertyInspectorTable.class.getName());
    assert inspector != null;
    final Property property = inspector.getSelectedProperty();
    assert property != null;
    doResetValue(selection, property, editor);
  }

  public static void doResetValue(final List<RadComponent> selection, final Property property, final GuiEditor editor) {
    try {
      if (!editor.ensureEditable()) return;
      final PropertyInspector propertyInspector = UIDesignerToolWindowManager.getInstance(editor.getProject()).getPropertyInspector();
      if (propertyInspector.isEditing()) {
        propertyInspector.stopEditing();
      }
      //noinspection unchecked
      for(RadComponent component: selection) {
        //noinspection unchecked
        if (property.isModified(component)) {
          //noinspection unchecked
          property.resetValue(component);
          component.getDelegee().invalidate();
        }
      }
      editor.refreshAndSave(false);
      propertyInspector.repaint();
    }
    catch (Exception e1) {
      LOG.error(e1);
    }
  }

  protected void update(final GuiEditor editor, final ArrayList<RadComponent> selection, final AnActionEvent e) {
    PropertyInspectorTable inspector = (PropertyInspectorTable)e.getDataContext().getData(PropertyInspectorTable.class.getName());
    if (inspector != null) {
      final Property selectedProperty = inspector.getSelectedProperty();
      //noinspection unchecked
      e.getPresentation().setEnabled(selectedProperty != null &&
                                     selection.size() > 0 &&
                                     inspector.isModifiedForSelection(selectedProperty));
    }
    else {
      e.getPresentation().setEnabled(false);
    }
  }
}