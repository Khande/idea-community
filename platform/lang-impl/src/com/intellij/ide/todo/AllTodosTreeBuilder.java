/**
 * @author Vladimir Kondratyev
 */
package com.intellij.ide.todo;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;

public class AllTodosTreeBuilder extends TodoTreeBuilder{
  public AllTodosTreeBuilder(JTree tree,DefaultTreeModel treeModel,Project project){
    super(tree,treeModel,project);
  }

  protected TodoTreeStructure createTreeStructure(){
    return new AllTodosTreeStructure(myProject);
  }

  void rebuildCache(){
    myFileTree.clear();
    myDirtyFileSet.clear();
    myFile2Highlighter.clear();

    TodoTreeStructure treeStructure=getTodoTreeStructure();
    PsiFile[] psiFiles= mySearchHelper.findFilesWithTodoItems();
    for (PsiFile psiFile : psiFiles) {
      if (mySearchHelper.getTodoItemsCount(psiFile) > 0 && treeStructure.accept(psiFile)) {
        myFileTree.add(psiFile.getVirtualFile());
      }
    }

    treeStructure.validateCache();
  }
}