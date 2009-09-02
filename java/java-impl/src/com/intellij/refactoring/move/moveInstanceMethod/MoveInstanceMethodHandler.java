package com.intellij.refactoring.move.moveInstanceMethod;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.impl.source.jsp.jspJava.JspClass;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.move.MoveInstanceMembersUtil;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author ven
 */
public class MoveInstanceMethodHandler implements RefactoringActionHandler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodHandler");
  static final String REFACTORING_NAME = RefactoringBundle.message("move.instance.method.title");

  public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    PsiElement element = LangDataKeys.PSI_ELEMENT.getData(dataContext);
    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    if (element == null) {
      element = file.findElementAt(editor.getCaretModel().getOffset());
    }

    if (element == null) return;
    if (element instanceof PsiIdentifier) element = element.getParent();

    if (!(element instanceof PsiMethod)) {
      String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("error.wrong.caret.position.method"));
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.MOVE_INSTANCE_METHOD);
      return;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Move Instance Method invoked");
    }
    invoke(project, new PsiElement[]{element}, dataContext);
  }

  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
    if (elements.length != 1 || !(elements[0] instanceof PsiMethod)) return;
    final PsiMethod method = (PsiMethod)elements[0];
    String message = null;
    if (method.isConstructor()) {
      message = RefactoringBundle.message("move.method.is.not.supported.for.constructors");
    }
    else if (PsiUtil.typeParametersIterator(method.getContainingClass()).hasNext() && TypeParametersSearcher.hasTypeParameters(method)) {
      message = RefactoringBundle.message("move.method.is.not.supported.for.generic.classes");
    }
    else if (method.findSuperMethods().length > 0 ||
             OverridingMethodsSearch.search(method, method.getUseScope(), true).toArray(PsiMethod.EMPTY_ARRAY).length > 0) {
      message = RefactoringBundle.message("move.method.is.not.supported.when.method.is.part.of.inheritance.hierarchy");
    }
    else {
      final Set<PsiClass> classes = MoveInstanceMembersUtil.getThisClassesToMembers(method).keySet();
      for (PsiClass aClass : classes) {
        if (aClass instanceof JspClass) {
          message = RefactoringBundle.message("synthetic.jsp.class.is.referenced.in.the.method");
          Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);
          CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.MOVE_INSTANCE_METHOD);
          break;
        }
      }
    }

    List<PsiVariable> suitableVariables = new ArrayList<PsiVariable>();
    if (message == null) {
      List<PsiVariable> allVariables = new ArrayList<PsiVariable>();
      allVariables.addAll(Arrays.asList(method.getParameterList().getParameters()));
      allVariables.addAll(Arrays.asList(method.getContainingClass().getFields()));
      boolean classTypesFound = false;
      boolean resolvableClassesFound = false;
      boolean classesInProjectFound = false;
      for (PsiVariable variable : allVariables) {
        final PsiType type = variable.getType();
        if (type instanceof PsiClassType && !((PsiClassType)type).hasParameters()) {
          classTypesFound = true;
          final PsiClass psiClass = ((PsiClassType)type).resolve();
          if (psiClass != null && !(psiClass instanceof PsiTypeParameter)) {
            resolvableClassesFound = true;
            final boolean inProject = method.getManager().isInProject(psiClass);
            if (inProject) {
              classesInProjectFound = true;
              suitableVariables.add(variable);
            }
          }
        }
      }

      if (suitableVariables.isEmpty()) {
        if (!classTypesFound) {
          message = RefactoringBundle.message("there.are.no.variables.that.have.reference.type");
        }
        else if (!resolvableClassesFound) {
          message = RefactoringBundle.message("all.candidate.variables.have.unknown.types");
        }
        else if (!classesInProjectFound) {
          message = RefactoringBundle.message("all.candidate.variables.have.types.not.in.project");
        }
      }
    }

    if (message != null) {
      Editor editor = dataContext == null ? null : PlatformDataKeys.EDITOR.getData(dataContext);
      CommonRefactoringUtil.showErrorHint(project, editor, RefactoringBundle.getCannotRefactorMessage(message), REFACTORING_NAME, HelpID.MOVE_INSTANCE_METHOD);
      return;
    }

    new MoveInstanceMethodDialog(
      method,
      suitableVariables.toArray(new PsiVariable[suitableVariables.size()])).show();
  }

  public static String suggestParameterNameForThisClass(final PsiClass thisClass) {
    PsiManager manager = thisClass.getManager();
    PsiType type = JavaPsiFacade.getInstance(manager.getProject()).getElementFactory().createType(thisClass);
    final SuggestedNameInfo suggestedNameInfo = JavaCodeStyleManager.getInstance(manager.getProject())
      .suggestVariableName(VariableKind.PARAMETER, null, null, type);
    return suggestedNameInfo.names.length > 0 ? suggestedNameInfo.names[0] : "";
  }

  public static Map<PsiClass, String> suggestParameterNames(final PsiMethod method, final PsiVariable targetVariable) {
    final Map<PsiClass, Set<PsiMember>> classesToMembers = MoveInstanceMembersUtil.getThisClassesToMembers(method);
    Map<PsiClass, String> result = new LinkedHashMap<PsiClass, String>();
    for (Map.Entry<PsiClass, Set<PsiMember>> entry : classesToMembers.entrySet()) {
      PsiClass aClass = entry.getKey();
      final Set<PsiMember> members = entry.getValue();
      if (members.size() == 1 && members.contains(targetVariable)) continue;
      result.put(aClass, suggestParameterNameForThisClass(aClass));
    }
    return result;
  }

  private static class TypeParametersSearcher extends PsiTypeVisitor<Boolean> {
    public static boolean hasTypeParameters(PsiElement element) {
      final TypeParametersSearcher searcher = new TypeParametersSearcher();
      final boolean[] hasParameters = new boolean[]{false};
      element.accept(new JavaRecursiveElementWalkingVisitor(){
        @Override
        public void visitTypeElement(PsiTypeElement type) {
          super.visitTypeElement(type);
          hasParameters[0] |= type.getType().accept(searcher);
        }
      });
      return hasParameters[0];
    }

    @Override
    public Boolean visitClassType(PsiClassType classType) {
      final PsiClass psiClass = PsiUtil.resolveClassInType(classType);
      if (psiClass instanceof PsiTypeParameter) {
        return Boolean.TRUE;
      }
      return super.visitClassType(classType);
    }

    @Override
    public Boolean visitWildcardType(PsiWildcardType wildcardType) {
      final PsiType bound = wildcardType.getBound();
      if (PsiUtil.resolveClassInType(bound) instanceof PsiTypeParameter) {
        return Boolean.TRUE;
      }
      return super.visitWildcardType(wildcardType);
    }

    @Override
    public Boolean visitType(PsiType type) {
      return Boolean.FALSE;
    }
  }
}