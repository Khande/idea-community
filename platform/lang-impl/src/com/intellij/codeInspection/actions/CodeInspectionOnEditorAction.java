package com.intellij.codeInspection.actions;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiFile;

public class CodeInspectionOnEditorAction extends AnAction {
  public void actionPerformed(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    Project project = PlatformDataKeys.PROJECT.getData(dataContext);
    if (project == null){
      return;
    }
    PsiFile psiFile = LangDataKeys.PSI_FILE.getData(dataContext);
    if (psiFile != null){
      analyze(project, psiFile);
    }
  }

  protected static void analyze(Project project, PsiFile psiFile) {
    FileDocumentManager.getInstance().saveAllDocuments();
    final InspectionManagerEx inspectionManagerEx = (InspectionManagerEx)InspectionManager.getInstance(project);
    final AnalysisScope scope = new AnalysisScope(psiFile);
    final GlobalInspectionContextImpl inspectionContext = inspectionManagerEx.createNewGlobalContext(false);
    inspectionContext.setCurrentScope(scope);
    final InspectionProfile inspectionProfile =
      InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
    inspectionContext.setExternalProfile(inspectionProfile);
    inspectionContext.doInspections(scope, inspectionManagerEx);    
  }

  public void update(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = PlatformDataKeys.PROJECT.getData(dataContext);
    final PsiFile psiFile = LangDataKeys.PSI_FILE.getData(dataContext);
    e.getPresentation().setEnabled(project != null && psiFile != null  && DaemonCodeAnalyzer.getInstance(project).isHighlightingAvailable(psiFile));
  }
}