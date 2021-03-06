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
package com.intellij.psi.util.proximity;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.NullableLazyKey;
import com.intellij.psi.*;
import com.intellij.psi.util.ProximityLocation;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.NullableFunction;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
*/
public class ExplicitlyImportedWeigher extends ProximityWeigher {
  private static final NullableLazyKey<PsiPackage, ProximityLocation>
    PLACE_PACKAGE = NullableLazyKey.create("placePackage", new NullableFunction<ProximityLocation, PsiPackage>() {
    @Override
    public PsiPackage fun(ProximityLocation location) {
      return PsiTreeUtil.getContextOfType(location.getPosition(), PsiPackage.class, false);
    }
  });

  public Comparable weigh(@NotNull final PsiElement element, @NotNull final ProximityLocation location) {
    final PsiElement position = location.getPosition();
    if (position == null){
      return null;
    }

    final PsiFile elementFile = element.getContainingFile();
    final PsiFile positionFile = position.getContainingFile();
    if (positionFile != null && elementFile != null && positionFile.getOriginalFile().equals(elementFile.getOriginalFile())) {
      return true;
    }

    if (element instanceof PsiClass) {
      final String qname = ((PsiClass) element).getQualifiedName();
      if (qname != null) {
        final PsiJavaFile psiJavaFile = PsiTreeUtil.getContextOfType(position, PsiJavaFile.class, false);
        if (psiJavaFile != null) {
          final PsiImportList importList = psiJavaFile.getImportList();
          if (importList != null) {
            for (final PsiImportStatement importStatement : importList.getImportStatements()) {
              final boolean onDemand = importStatement.isOnDemand();
              final String imported = importStatement.getQualifiedName();
              if (onDemand && qname.startsWith(imported + ".") || !onDemand && qname.equals(imported)) {
                return true;
              }
            }
          }
        }
      }

    }
    if (element instanceof PsiMember) {
      final PsiPackage placePackage = PLACE_PACKAGE.getValue(location);
      if (placePackage == null) {
        return false;
      }

      Module elementModule = ModuleUtil.findModuleForPsiElement(element);
      return location.getPositionModule() == elementModule &&
             placePackage.equals(PsiTreeUtil.getContextOfType(element, PsiPackage.class, false));
    }
    return false;
  }
}
