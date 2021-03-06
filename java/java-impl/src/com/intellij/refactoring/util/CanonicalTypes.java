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
package com.intellij.refactoring.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NonNls;

import java.util.Collection;
import java.util.Map;

/**
 * @author dsl
 */
public class CanonicalTypes {
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.util.CanonicalTypes");
  public abstract static class Type {
    public abstract PsiType getType(PsiElement context, final PsiManager manager) throws IncorrectOperationException;

    @NonNls
    public abstract String getTypeText();

    public abstract void addImportsTo(final JavaCodeFragment codeFragment);
  }

  private static class Primitive extends Type {
    private final PsiPrimitiveType myType;
    private Primitive(PsiPrimitiveType type) {
      myType = type;
    }

    public PsiType getType(PsiElement context, final PsiManager manager) {
      return myType;
    }

    public String getTypeText() {
      return myType.getPresentableText();
    }

    public void addImportsTo(final JavaCodeFragment codeFragment) {}
  }

  private static class Array extends Type {
    private final Type myComponentType;

    private Array(Type componentType) {
      myComponentType = componentType;
    }

    public PsiType getType(PsiElement context, final PsiManager manager) throws IncorrectOperationException {
      return myComponentType.getType(context, manager).createArrayType();
    }

    public String getTypeText() {
      return myComponentType.getTypeText() + "[]";
    }

    public void addImportsTo(final JavaCodeFragment codeFragment) {
      myComponentType.addImportsTo(codeFragment);
    }
  }

  private static class Ellipsis extends Type {
    private final Type myComponentType;

    private Ellipsis(Type componentType) {
      myComponentType = componentType;
    }

    public PsiType getType(PsiElement context, final PsiManager manager) throws IncorrectOperationException {
      return new PsiEllipsisType(myComponentType.getType(context, manager));
    }

    public String getTypeText() {
      return myComponentType.getTypeText() + "...";
    }

    public void addImportsTo(final JavaCodeFragment codeFragment) {
      myComponentType.addImportsTo(codeFragment);
    }
  }

  private static class WildcardType extends Type {
    private final boolean myIsExtending;
    private final Type myBound;

    private WildcardType(boolean isExtending, Type bound) {
      myIsExtending = isExtending;
      myBound = bound;
    }

    public PsiType getType(PsiElement context, final PsiManager manager) throws IncorrectOperationException {
      if(myBound == null) return PsiWildcardType.createUnbounded(context.getManager());
      if (myIsExtending) {
        return PsiWildcardType.createExtends(context.getManager(), myBound.getType(context, manager));
      }
      else {
        return PsiWildcardType.createSuper(context.getManager(), myBound.getType(context, manager));
      }
    }

    public String getTypeText() {
      if (myBound == null) return "?";
      return "? " + (myIsExtending ? "extends " : "super ") + myBound.getTypeText();
    }

    public void addImportsTo(final JavaCodeFragment codeFragment) {
      if (myBound != null) myBound.addImportsTo(codeFragment);
    }
  }

  private static class WrongType extends Type {
    private final String myText;

    private WrongType(String text) {
      myText = text;
    }

    public PsiType getType(PsiElement context, final PsiManager manager) throws IncorrectOperationException {
      return JavaPsiFacade.getInstance(context.getProject()).getElementFactory().createTypeFromText(myText, context);
    }

    public String getTypeText() {
      return myText;
    }

    public void addImportsTo(final JavaCodeFragment codeFragment) {}
  }


  private static class ClassType extends Type {
    private final String myOriginalText;
    private final String myClassQName;
    private final Map<String,Type> mySubstitutor;

    private ClassType(String originalText, String classQName, Map<String, Type> substitutor) {
      myOriginalText = originalText;
      myClassQName = classQName;
      mySubstitutor = substitutor;
    }

    public PsiType getType(PsiElement context, final PsiManager manager) throws IncorrectOperationException {
      final JavaPsiFacade facade = JavaPsiFacade.getInstance(manager.getProject());
      final PsiElementFactory factory = facade.getElementFactory();
      final PsiResolveHelper resolveHelper = facade.getResolveHelper();
      final PsiClass aClass = resolveHelper.resolveReferencedClass(myClassQName, context);
      if (aClass == null) {
        return factory.createTypeFromText(myClassQName, context);
      }
      Map<PsiTypeParameter, PsiType> substMap = new HashMap<PsiTypeParameter,PsiType>();
      for (PsiTypeParameter typeParameter : PsiUtil.typeParametersIterable(aClass)) {
        final String name = typeParameter.getName();
        final Type type = mySubstitutor.get(name);
        if (type != null) {
          substMap.put(typeParameter, type.getType(context, manager));
        } else {
          substMap.put(typeParameter, null);
        }
      }
      return factory.createType(aClass, factory.createSubstitutor(substMap));
    }

    public String getTypeText() {
      return myOriginalText;
    }

    public void addImportsTo(final JavaCodeFragment codeFragment) {
      codeFragment.addImportsFromString(myClassQName);
      final Collection<Type> types = mySubstitutor.values();
      for (Type type : types) {
        if (type != null) {
          type.addImportsTo(codeFragment);
        }
      }
    }
  }

  private static class Creator extends PsiTypeVisitor<Type> {
    public static final Creator INSTANCE = new Creator();
    public Type visitPrimitiveType(PsiPrimitiveType primitiveType) {
      return new Primitive(primitiveType);
    }

    public Type visitEllipsisType(PsiEllipsisType ellipsisType) {
      return new Ellipsis(ellipsisType.getComponentType().accept(this));
    }

    public Type visitArrayType(PsiArrayType arrayType) {
      return new Array(arrayType.getComponentType().accept(this));
    }

    public Type visitWildcardType(PsiWildcardType wildcardType) {
      final PsiType wildcardBound = wildcardType.getBound();
      final Type bound = wildcardBound == null ? null : wildcardBound.accept(this);
      return new WildcardType(wildcardType.isExtends(), bound);
    }

    public Type visitClassType(PsiClassType classType) {
      final PsiClassType.ClassResolveResult resolveResult = classType.resolveGenerics();
      final PsiClass aClass = resolveResult.getElement();
      if (aClass instanceof PsiAnonymousClass) {
        return visitClassType(((PsiAnonymousClass)aClass).getBaseClassType());
      }
      final String originalText = classType.getPresentableText();
      if (aClass == null) {
        return new WrongType(originalText);
      } else {
        Map<String,Type> substMap = new HashMap<String,Type>();
        final PsiSubstitutor substitutor = resolveResult.getSubstitutor();
        for (PsiTypeParameter typeParameter : PsiUtil.typeParametersIterable(aClass)) {
          final PsiType substType = substitutor.substitute(typeParameter);
          final String name = typeParameter.getName();
          if (substType == null) {
            substMap.put(name, null);
          } else {
            substMap.put(name, substType.accept(this));
          }
        }
        final String qualifiedName = aClass.getQualifiedName();
        LOG.assertTrue(aClass.getName() != null);
        return new ClassType(originalText, qualifiedName != null ? qualifiedName : aClass.getName(), substMap);
      }
    }
  }

  public static Type createTypeWrapper(PsiType type) {
    return type.accept(Creator.INSTANCE);
  }
}
