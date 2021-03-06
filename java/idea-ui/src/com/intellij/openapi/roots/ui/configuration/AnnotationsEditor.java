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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.AnnotationOrderRootType;
import com.intellij.openapi.roots.ui.util.CellAppearance;
import com.intellij.openapi.roots.ui.util.CellAppearanceUtils;
import com.intellij.openapi.roots.ui.util.SimpleTextCellAppearance;
import com.intellij.openapi.ui.PanelWithText;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TableUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.ItemRemovable;
import com.intellij.util.ui.Table;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 * Date: Oct 4, 2003
 * Time: 6:54:57 PM
 */
public class AnnotationsEditor extends ModuleElementsEditor {
  private JTable myTable;
  private JButton myAddPathButton;
  private JButton myRemoveButton;

  public static final String NAME = ProjectBundle.message("project.roots.external.annotations.tab.title");
  public static final Icon ICON = IconLoader.getIcon("/modules/annotation.png");

  public AnnotationsEditor(final ModuleConfigurationState state) {
    super(state);
  }

  public String getHelpTopic() {
    return "project.paths.annotations";//todo
  }

  public String getDisplayName() {
    return NAME;
  }

  public Icon getIcon() {
    return ICON;
  }

  public void saveData() {
    TableUtil.stopEditing(myTable);
    final int count = myTable.getRowCount();
    String[] urls = ArrayUtil.newStringArray(count);
    for (int row = 0; row < count; row++) {
      final TableItem item = ((MyTableModel)myTable.getModel()).getTableItemAt(row);
      urls[row] = item.getUrl();
    }
    getModel().setRootUrls(AnnotationOrderRootType.getInstance(), urls);
  }

  public JComponent createComponentImpl() {
    final JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
    mainPanel.setPreferredSize(new Dimension(-1, 200));
    final DefaultTableModel tableModel = createModel();
    myTable = new Table(tableModel);
    myTable.setIntercellSpacing(new Dimension(0, 0));
    myTable.setDefaultRenderer(TableItem.class, new MyRenderer());
    myTable.setShowGrid(false);
    myTable.setDragEnabled(false);
    myTable.setShowHorizontalLines(false);
    myTable.setShowVerticalLines(false);
    myTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    myAddPathButton = new JButton(ProjectBundle.message("module.javadoc.add.path.button"));
    myAddPathButton.addActionListener(new AddPathActionListener());


    myRemoveButton = new JButton(ProjectBundle.message("module.javadoc.remove.button"));
    myRemoveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final List removedItems = TableUtil.removeSelectedItems(myTable);
        if (removedItems.size() > 0) {
          saveData();
        }
      }
    });

    final JPanel panel = new JPanel(new GridBagLayout());
    panel.add(myAddPathButton, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 6, 0, 0), 0, 0));
    panel.add(myRemoveButton, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(6, 6, 0, 0), 0, 0));

    mainPanel.add(ScrollPaneFactory.createScrollPane(myTable), BorderLayout.CENTER);
    mainPanel.add(panel, BorderLayout.EAST);
    final PanelWithText panelWithText = new PanelWithText(ProjectBundle.message("project.roots.external.annotations.description"));
    panelWithText.setBorder(null);
    mainPanel.add(panelWithText, BorderLayout.NORTH);

    myTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
          return;
        }
        final int selectedIndex = myTable.getSelectedRow();
        myRemoveButton.setEnabled(selectedIndex >= 0);
      }
    });
    if (tableModel.getRowCount() > 0) {
      TableUtil.selectRows(myTable, new int[] {0});
    }
    else {
      myRemoveButton.setEnabled(false);
    }
    return mainPanel;
  }

  protected DefaultTableModel createModel() {
    final MyTableModel tableModel = new MyTableModel();
    final String[] urls = getModel().getRootUrls(AnnotationOrderRootType.getInstance());
    for (String javadocUrl : urls) {
      tableModel.addTableItem(new TableItem(javadocUrl));
    }
    return tableModel;
  }

  public void moduleStateChanged() {
    if (myTable != null) {
      final DefaultTableModel tableModel = createModel();
      myTable.setModel(tableModel);
      myRemoveButton.setEnabled(tableModel.getRowCount() > 0);
    }
  }

  private static class MyRenderer extends ColoredTableCellRenderer {
    private static final Border NO_FOCUS_BORDER = BorderFactory.createEmptyBorder(1, 1, 1, 1);

    protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
      setPaintFocusBorder(false);
      setFocusBorderAroundIcon(true);
      setBorder(NO_FOCUS_BORDER);

      final TableItem tableItem = ((TableItem)value);
      tableItem.getCellAppearance().customize(this);
    }
  }

  private static class TableItem {
    private final String myUrl;
    private final CellAppearance myCellAppearance;
    public TableItem(VirtualFile file) {
      myUrl = file.getUrl();
      myCellAppearance = CellAppearanceUtils.forVirtualFile(file);
    }

    public TableItem(String url) {
      myUrl = url;
      final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
      if (file != null) {
        myCellAppearance = CellAppearanceUtils.forVirtualFile(file);
      }
      else {
        myCellAppearance = SimpleTextCellAppearance.invalid(url, CellAppearanceUtils.INVALID_ICON);
      }
    }

    public String getUrl() {
      return myUrl;
    }

    public CellAppearance getCellAppearance() {
      return myCellAppearance;
    }
  }

  private static class MyTableModel extends DefaultTableModel implements ItemRemovable{
    public String getColumnName(int column) {
      return null;
    }

    public Class getColumnClass(int columnIndex) {
      return TableItem.class;
    }

    public int getColumnCount() {
      return 1;
    }

    public boolean isCellEditable(int row, int column) {
      return false;
    }

    public TableItem getTableItemAt(int row) {
      return (TableItem)getValueAt(row, 0);
    }

    public void addTableItem(TableItem item) {
      addRow(new Object[] {item});
    }
  }

  private abstract class MyAddAction implements ActionListener {
    protected abstract VirtualFile[] getFiles();

    public void actionPerformed(ActionEvent e) {
      VirtualFile[] files = getFiles();
      final MyTableModel tableModel = (MyTableModel)myTable.getModel();
      boolean changes = false;
      for (final VirtualFile file : files) {
        if (file != null) {
          tableModel.addTableItem(new TableItem(file));
          changes = true;
        }
      }
      if (changes) {
        saveData();
        TableUtil.selectRows(myTable, new int[] {tableModel.getRowCount() - 1});
      }
    }
  }

  private class AddPathActionListener extends MyAddAction{
    private final FileChooserDescriptor myDescriptor;

    public AddPathActionListener() {
      myDescriptor = new FileChooserDescriptor(false, true, true, false, true, true);
      myDescriptor.setTitle(ProjectBundle.message("add.external.annotations.path.title"));
      myDescriptor.setDescription(ProjectBundle.message("add.external.annotations.path.description"));
    }

    protected VirtualFile[] getFiles() {
      return FileChooser.chooseFiles(myTable, myDescriptor);
    }
  }
}
