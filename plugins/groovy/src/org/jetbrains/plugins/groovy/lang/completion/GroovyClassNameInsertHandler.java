/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package org.jetbrains.plugins.groovy.lang.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrNewExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrAnonymousClassDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.imports.GrImportStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrCodeReferenceElement;

/**
 * @author Maxim.Medvedev
 */
public class GroovyClassNameInsertHandler implements InsertHandler<JavaPsiClassReferenceElement> {
  public static boolean isReferenceInNewExpression(PsiElement reference) {
    if (!(reference instanceof GrCodeReferenceElement)) return false;

    PsiElement parent = reference.getParent();
    while (parent instanceof GrCodeReferenceElement) parent = parent.getParent();
    if (parent instanceof GrAnonymousClassDefinition) parent = parent.getParent();
    return parent instanceof GrNewExpression;
  }

  @Override
  public void handleInsert(InsertionContext context, JavaPsiClassReferenceElement item) {
    PsiFile file = context.getFile();
    Editor editor = context.getEditor();
    int endOffset = editor.getCaretModel().getOffset();
    if (PsiTreeUtil.findElementOfClassAtOffset(file, endOffset - 1, GrImportStatement.class, false) != null) {
      AllClassesGetter.INSERT_FQN.handleInsert(context, item);
      return;
    }
    PsiElement position = file.findElementAt(endOffset - 1);

    final boolean inNew = position != null && isReferenceInNewExpression(position.getParent());

    final PsiClass psiClass = item.getObject();
    if (isInVariable(position) || GroovyCompletionContributor.isInClosurePropertyParameters(position)) {
      Project project = context.getProject();
      String qname = psiClass.getQualifiedName();
      String shortName = psiClass.getName();
      if (qname == null) return;

      PsiClass aClass = JavaPsiFacade.getInstance(project).getResolveHelper().resolveReferencedClass(shortName, position);
      if (aClass == null) {
        ((GroovyFileBase)file).addImportForClass(psiClass);
        return;
      }
      else if (aClass == JavaCompletionUtil.getOriginalElement(psiClass)) {
        return;
      }
    }
    AllClassesGetter.TRY_SHORTENING.handleInsert(context, item);

    if (inNew && !JavaCompletionUtil.hasAccessibleInnerClass(psiClass, file)) {
      JavaCompletionUtil.insertParentheses(context, item, false, GroovyCompletionUtil.hasConstructorParameters(psiClass));
    }

  }

  private static boolean isInVariable(PsiElement position) {
    GrVariable variable = PsiTreeUtil.getParentOfType(position, GrVariable.class);
    return variable != null && variable.getTypeElementGroovy() == null && position == variable.getNameIdentifierGroovy();
  }
}
