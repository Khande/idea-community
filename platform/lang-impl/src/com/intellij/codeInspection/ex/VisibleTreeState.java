package com.intellij.codeInspection.ex;

import com.intellij.profile.codeInspection.ui.InspectionConfigTreeNode;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.intellij.util.xmlb.annotations.Tag;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;

/**
 * User: anna
 * Date: Dec 18, 2004
 */
@Tag("profile-state")
public class VisibleTreeState{
  @Tag("expanded-state")
  @AbstractCollection(surroundWithTag = false, elementTag = "expanded", elementValueAttribute = "path", elementTypes = {State.class})
  public TreeSet<State> myExpandedNodes = new TreeSet<State>();

  @Tag("selected-state")
  @AbstractCollection(surroundWithTag = false, elementTag = "selected", elementValueAttribute = "path", elementTypes = {State.class})
  public TreeSet<State> mySelectedNodes = new TreeSet<State>();

  public VisibleTreeState(VisibleTreeState src) {
    myExpandedNodes.addAll(src.myExpandedNodes);
    mySelectedNodes.addAll(src.mySelectedNodes);
  }

  public VisibleTreeState() {
  }

  public void expandNode(String nodeTitle) {
    myExpandedNodes.add(new State(nodeTitle));
  }

  public void collapseNode(String nodeTitle) {
    myExpandedNodes.remove(new State(nodeTitle));
  }

  public void restoreVisibleState(Tree tree) {
    ArrayList<TreePath> pathsToExpand = new ArrayList<TreePath>();
    ArrayList<TreePath> toSelect = new ArrayList<TreePath>();
    traverseNodes((DefaultMutableTreeNode)tree.getModel().getRoot(), pathsToExpand, toSelect);
    TreeUtil.restoreExpandedPaths(tree, pathsToExpand);
    if (toSelect.isEmpty()) {
      TreeUtil.selectFirstNode(tree);
    }
    else {
      for (final TreePath aToSelect : toSelect) {
        TreeUtil.selectPath(tree, aToSelect);
      }
    }
  }

  private void traverseNodes(final DefaultMutableTreeNode root, List<TreePath> pathsToExpand, List<TreePath> toSelect) {
    final Descriptor descriptor = ((InspectionConfigTreeNode)root).getDesriptor();
    final TreeNode[] rootPath = root.getPath();
    if (descriptor != null) {
      final String shortName = descriptor.getKey().toString();
      if (mySelectedNodes.contains(new State(descriptor))) {
        toSelect.add(new TreePath(rootPath));
      }
      if (myExpandedNodes.contains(new State(descriptor))) {
        pathsToExpand.add(new TreePath(rootPath));
      }
    }
    else {
      final String str = ((InspectionConfigTreeNode)root).getGroupName();
      if (mySelectedNodes.contains(new State(str))) {
        toSelect.add(new TreePath(rootPath));
      }
      if (myExpandedNodes.contains(new State(str))) {
        pathsToExpand.add(new TreePath(rootPath));
      }
    }
    for (int i = 0; i < root.getChildCount(); i++) {
      traverseNodes((DefaultMutableTreeNode)root.getChildAt(i), pathsToExpand, toSelect);
    }
  }

  public void saveVisibleState(Tree tree) {
    myExpandedNodes.clear();
    final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)tree.getModel().getRoot();
    Enumeration<TreePath> expanded = tree.getExpandedDescendants(new TreePath(rootNode.getPath()));
    if (expanded != null) {
      while (expanded.hasMoreElements()) {
        final TreePath treePath = expanded.nextElement();
        final InspectionConfigTreeNode node = (InspectionConfigTreeNode)treePath.getLastPathComponent();
        final Descriptor descriptor = node.getDesriptor();
        myExpandedNodes.add(getState(node, descriptor));
      }
    }

    setSelectionPaths(tree.getSelectionPaths());
  }

  private static State getState(InspectionConfigTreeNode node, Descriptor descriptor) {
    final State expandedNode;
    if (descriptor != null) {
      expandedNode = new State(descriptor);
    }
    else {
      expandedNode = new State(node.getGroupName());
    }
    return expandedNode;
  }

  public void setSelectionPaths(final TreePath[] selectionPaths) {
    mySelectedNodes.clear();
    if (selectionPaths != null) {
      for (TreePath selectionPath : selectionPaths) {
        final InspectionConfigTreeNode node = (InspectionConfigTreeNode)selectionPath.getLastPathComponent();
        final Descriptor descriptor = node.getDesriptor();
        mySelectedNodes.add(getState(node, descriptor));
      }
    }
  }


  public static class State implements Comparable{
    @Tag("id")
    public String myKey;
    Descriptor myDescriptor;

    public State(String key) {
      myKey = key;
    }

    public State(Descriptor descriptor) {
      myKey = descriptor.toString();
      myDescriptor = descriptor;
    }

    //readExternal
    public State(){
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      State state = (State)o;

      if (myKey != null ? !myKey.equals(state.myKey) : state.myKey != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = myKey != null ? myKey.hashCode() : 0;
      result = 31 * result + (myDescriptor != null ? myDescriptor.hashCode() : 0);
      return result;
    }

    public int compareTo(Object o) {
      if (!(o instanceof State)) return -1;
      final State other = (State)o;
      if (myKey.equals(other.myKey)) {
        if (myDescriptor != null && other.myDescriptor != null) {
          final NamedScope scope1 = myDescriptor.getScope();
          final NamedScope scope2 = other.myDescriptor.getScope();
          if (scope1 != null && scope2 != null) {
            return scope1.getName().compareTo(scope2.getName());
          }
        }
      }
      return myKey.compareTo(other.myKey);
    }
  }
}