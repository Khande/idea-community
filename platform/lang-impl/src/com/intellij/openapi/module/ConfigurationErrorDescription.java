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

package com.intellij.openapi.module;

/**
 * @author nik
 */
public abstract class ConfigurationErrorDescription {
  private final String myElementName;
  private final String myElementKind;
  private final String myDescription;

  protected ConfigurationErrorDescription(String elementName, String elementKind, String description) {
    myElementName = elementName;
    myElementKind = elementKind;
    myDescription = description;
  }

  public String getElementName() {
    return myElementName;
  }

  public String getElementKind() {
    return myElementKind;
  }

  public String getDescription() {
    return myDescription;
  }

  public abstract void removeInvalidElement();

  public abstract String getRemoveConfirmationMessage();

  public boolean isValid() {
    return true;
  }
}
