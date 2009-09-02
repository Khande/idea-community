/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.util.xml.highlighting;

import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NonNls;

/**
 * @author peter
 */
public class MockAnnotatingDomInspection<T extends DomElement> extends BasicDomElementsInspection<T>{

  public MockAnnotatingDomInspection(final Class<T> domClass) {
    super(domClass);
  }

  protected void checkDomElement(DomElement element, DomElementAnnotationHolder holder, DomHighlightingHelper helper) {
    for (final Class aClass : getDomClasses()) {
      helper.runAnnotators(element, holder, aClass);
    }
  }

  @Nls
  @NotNull
  public String getGroupDisplayName() {
    throw new UnsupportedOperationException("Method getGroupDisplayName is not yet implemented in " + getClass().getName());
  }

  @Nls
  @NotNull
  public String getDisplayName() {
    throw new UnsupportedOperationException("Method getDisplayName is not yet implemented in " + getClass().getName());
  }

  @NonNls
  @NotNull
  public String getShortName() {
    throw new UnsupportedOperationException("Method getShortName is not yet implemented in " + getClass().getName());
  }
}