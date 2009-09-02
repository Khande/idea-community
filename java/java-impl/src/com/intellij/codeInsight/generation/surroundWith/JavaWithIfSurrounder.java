
package com.intellij.codeInsight.generation.surroundWith;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;

public class JavaWithIfSurrounder extends JavaStatementsSurrounder{
  public String getTemplateDescription() {
    return CodeInsightBundle.message("surround.with.if.template");
  }

  public TextRange surroundStatements(Project project, Editor editor, PsiElement container, PsiElement[] statements) throws IncorrectOperationException{
    PsiManager manager = PsiManager.getInstance(project);
    PsiElementFactory factory = JavaPsiFacade.getInstance(manager.getProject()).getElementFactory();
    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);

    statements = SurroundWithUtil.moveDeclarationsOut(container, statements, true);
    if (statements.length == 0){
      return null;
    }

    @NonNls String text = "if(a){\n}";
    PsiIfStatement ifStatement = (PsiIfStatement)factory.createStatementFromText(text, null);
    ifStatement = (PsiIfStatement)codeStyleManager.reformat(ifStatement);

    ifStatement = (PsiIfStatement)container.addAfter(ifStatement, statements[statements.length - 1]);

    final PsiStatement thenBranch = ifStatement.getThenBranch();
    if (thenBranch != null) {
      PsiCodeBlock thenBlock = ((PsiBlockStatement)thenBranch).getCodeBlock();
      thenBlock.addRange(statements[0], statements[statements.length - 1]);
      container.deleteChildRange(statements[0], statements[statements.length - 1]);
    }

    ifStatement = CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(ifStatement);
    if (ifStatement == null) {
      return null;
    }

    final PsiExpression condition = ifStatement.getCondition();
    if (condition != null) {
      TextRange range = condition.getTextRange();
      TextRange textRange = new TextRange(range.getStartOffset(), range.getStartOffset());
      editor.getDocument().deleteString(range.getStartOffset(), range.getEndOffset());
      return textRange;
    }
    return ifStatement.getTextRange();
  }
}