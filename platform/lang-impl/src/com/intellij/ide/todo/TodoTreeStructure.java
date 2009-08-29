package com.intellij.ide.todo;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeStructureBase;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.TodoPattern;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Vladimir Kondratyev
 */
public abstract class TodoTreeStructure extends AbstractTreeStructureBase implements ToDoSettings{
  protected TodoTreeBuilder myBuilder;
  protected AbstractTreeNode myRootElement;
  protected final ToDoSummary mySummaryElement;

  private boolean myFlattenPackages;
  protected boolean myArePackagesShown;
  private boolean myAreModulesShown;


  protected final PsiSearchHelper mySearchHelper;
  /**
   * Current <code>TodoFilter</code>. If no filter is set then this field is <code>null</code>.
   */
  protected TodoFilter myTodoFilter;

  public TodoTreeStructure(Project project){
    super(project);
    myArePackagesShown=true;
    mySummaryElement=new ToDoSummary();
    PsiManager psiManager = PsiManager.getInstance(project);
    mySearchHelper= psiManager.getSearchHelper();
  }

  final void setTreeBuilder(TodoTreeBuilder builder){
    myBuilder=builder;
    myRootElement=createRootElement();
  }

  protected abstract AbstractTreeNode createRootElement();

  public abstract boolean accept(PsiFile psiFile);

  /**
   * Validate whole the cache
   */
  protected void validateCache(){
  }

  public final boolean isPackagesShown(){
    return myArePackagesShown;
  }

  final void setShownPackages(boolean state){
    myArePackagesShown=state;
  }

  public final boolean areFlattenPackages(){
    return myFlattenPackages;
  }

  public final void setFlattenPackages(boolean state){
    myFlattenPackages=state;
  }

  /**
   * Sets new <code>TodoFilter</code>. <code>null</code> is acceptable value. It means
   * that there is no any filtration of <code>TodoItem>/code>s.
   */
  final void setTodoFilter(TodoFilter todoFilter){
    myTodoFilter=todoFilter;
  }

  /**
   * @return first element that can be selected in the tree. The method can returns <code>null</code>.
   */
  abstract Object getFirstSelectableElement();

  /**
   * @return number of <code>TodoItem</code>s located in the file.
   */
  public final int getTodoItemCount(PsiFile psiFile){
    int count=0;
    if(psiFile != null){
      if(myTodoFilter!=null){
        for(Iterator i=myTodoFilter.iterator();i.hasNext();){
          TodoPattern pattern=(TodoPattern)i.next();
          count+=mySearchHelper.getTodoItemsCount(psiFile,pattern);
        }
      }else{
        count=mySearchHelper.getTodoItemsCount(psiFile);
      }
    }
    return count;
  }

  boolean isAutoExpandNode(NodeDescriptor descriptor){
    Object element=descriptor.getElement();
    return element == getRootElement() || element == mySummaryElement;
  }

  public final void commit() {
    PsiDocumentManager.getInstance(myProject).commitAllDocuments();
  }

  public boolean hasSomethingToCommit() {
    return PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments();
  }

  public final Object getRootElement(){
    return myRootElement;
  }

  public boolean getIsFlattenPackages() {
    return myFlattenPackages;
  }

  public PsiSearchHelper getSearchHelper() {
    return mySearchHelper;
  }

  public TodoFilter getTodoFilter() {
    return myTodoFilter;
  }

  public List<TreeStructureProvider> getProviders() {
    return Collections.emptyList();
  }

  void setShownModules(boolean state) {
    myAreModulesShown = state;
  }

  public boolean isModulesShown() {
    return myAreModulesShown;
  }
}