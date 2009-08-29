/*
 * Copyright (c) 2000-2007 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.patterns;

import com.intellij.psi.PsiField;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.Nullable;

/**
 * @author peter
 */
public class PsiFieldPattern extends PsiMemberPattern<PsiField, PsiFieldPattern>{
  public PsiFieldPattern() {
    super(new InitialPatternCondition<PsiField>(PsiField.class) {
      public boolean accepts(@Nullable final Object o, final ProcessingContext context) {
        return o instanceof PsiField;
      }
    });
  }
}