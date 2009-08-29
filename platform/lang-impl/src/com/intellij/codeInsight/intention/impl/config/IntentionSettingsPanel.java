package com.intellij.codeInsight.intention.impl.config;

import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.ide.ui.search.SearchableOptionsRegistrar;
import com.intellij.openapi.options.MasterDetails;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.DetailsComponent;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.GuiUtils;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IntentionSettingsPanel implements MasterDetails {
  private JPanel myPanel;
  private final IntentionSettingsTree myIntentionSettingsTree;
  private final IntentionDescriptionPanel myIntentionDescriptionPanel = new IntentionDescriptionPanel();

  private JPanel myTreePanel;
  private JPanel myDescriptionPanel;
  private DetailsComponent myDetailsComponent;

  public IntentionSettingsPanel() {
    myIntentionSettingsTree = new IntentionSettingsTree() {
      protected void selectionChanged(Object selected) {
        if (selected instanceof IntentionActionMetaData) {
          IntentionActionMetaData actionMetaData = (IntentionActionMetaData)selected;
          intentionSelected(actionMetaData);
          if (myDetailsComponent != null) {
            String[] text = new String[actionMetaData.myCategory.length + 1];
            System.arraycopy(actionMetaData.myCategory, 0, text,0,actionMetaData.myCategory.length);
            text[text.length - 1] = actionMetaData.getFamily();
            myDetailsComponent.setText(text);
          }
        }
        else {
          categorySelected((String)selected);
          if (myDetailsComponent != null) {
            myDetailsComponent.setText((String)selected);
          }
        }
      }

      protected List<IntentionActionMetaData> filterModel(String filter, final boolean force) {
        final List<IntentionActionMetaData> list = IntentionManagerSettings.getInstance().getMetaData();
        if (filter == null || filter.length() == 0) return list;
        final HashSet<String> quoted = new HashSet<String>();
        List<Set<String>> keySetList = SearchUtil.findKeys(filter, quoted);
        List<IntentionActionMetaData> result = new ArrayList<IntentionActionMetaData>();
        for (IntentionActionMetaData metaData : list) {
          if (isIntentionAccepted(metaData, filter, force, keySetList, quoted)){
            result.add(metaData);
          }
        }
        final Set<String> filters = SearchableOptionsRegistrar.getInstance().getProcessedWords(filter);
        if (force && result.isEmpty()){
          if (filters.size() > 1){
            result = filterModel(filter, false);
          }
        }
        return result;
      }
    };
    myTreePanel.setLayout(new BorderLayout());
    myTreePanel.add(myIntentionSettingsTree.getComponent(), BorderLayout.CENTER);

    GuiUtils.replaceJSplitPaneWithIDEASplitter(myPanel);

    myDescriptionPanel.setLayout(new BorderLayout());
    myDescriptionPanel.add(myIntentionDescriptionPanel.getComponent(), BorderLayout.CENTER);
  }

  private void intentionSelected(IntentionActionMetaData actionMetaData) {
    myIntentionDescriptionPanel.reset(actionMetaData, myIntentionSettingsTree.getFilter());
  }

  private void categorySelected(String intentionCategory) {
    myIntentionDescriptionPanel.reset(intentionCategory);
  }

  public void reset() {
    myIntentionSettingsTree.reset();
    SwingUtilities.invokeLater(new Runnable(){
      public void run() {
        myIntentionDescriptionPanel.init(myPanel.getWidth()/2);
      }
    });
  }

  public void initUi() {
    myDetailsComponent = new DetailsComponent();
    myDetailsComponent.setContent(myDescriptionPanel);
  }

  public JComponent getToolbar() {
    return myIntentionSettingsTree.getToolbarPanel();
  }

  public JComponent getMaster() {
    return myTreePanel;
  }

  public DetailsComponent getDetails() {
    return myDetailsComponent;
  }

  public void apply() {
    myIntentionSettingsTree.apply();
  }

  public JPanel getComponent() {
    return myPanel;
  }

  public JTree getIntentionTree(){
    return myIntentionSettingsTree.getTree();
  }

  public boolean isModified() {
    return myIntentionSettingsTree.isModified();
  }

  public void dispose() {
    myIntentionSettingsTree.dispose();
    myIntentionDescriptionPanel.dispose();
  }

  private static boolean isIntentionAccepted(IntentionActionMetaData metaData, @NonNls String filter, boolean forceInclude,
                                             final List<Set<String>> keySetList, final HashSet<String> quoted) {
    if (StringUtil.containsIgnoreCase(metaData.getFamily(), filter)) {
      return true;
    }
    for (String category : metaData.myCategory) {
      if (category != null && StringUtil.containsIgnoreCase(category, filter)) {
        return true;
      }
    }
    for (String stripped : quoted) {
      if (StringUtil.containsIgnoreCase(metaData.getFamily(), stripped)) {
        return true;
      }
      for (String category : metaData.myCategory) {
        if (category != null && StringUtil.containsIgnoreCase(category, stripped)) {
          return true;
        }
      }
      try {
        final TextDescriptor description = metaData.getDescription();
        if (description != null) {
          if (StringUtil.containsIgnoreCase(description.getText(), stripped)){
            if (!forceInclude) return true;
          } else if (forceInclude) return false;
        }
      }
      catch (IOException e) {
        //skip then
      }
    }
    for (Set<String> keySet : keySetList) {
      if (keySet.contains(metaData.getFamily())) {
        if (!forceInclude) {
          return true;
        }
      }
      else {
        if (forceInclude) {
          return false;
        }
      }
    }
    return forceInclude;
  }

  public Runnable showOption(final SearchableConfigurable configurable, final String option) {
    return new Runnable() {
      public void run() {
        myIntentionSettingsTree.filter(myIntentionSettingsTree.filterModel(option, true));
        myIntentionSettingsTree.setFilter(option);
      }
    };
  }

  public void clearSearch() {
  }
}