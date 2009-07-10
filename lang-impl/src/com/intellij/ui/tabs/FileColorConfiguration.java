package com.intellij.ui.tabs;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
* @author spleaner
*/
class FileColorConfiguration implements Cloneable {
  private static final String COLOR = "color";

  private String myScopeName;
  private String myColorName;
  private static final String SCOPE_NAME = "scope";

  public FileColorConfiguration() {
  }

  public FileColorConfiguration(final String scopeName, final String colorName) {
    myScopeName = scopeName;
    myColorName = colorName;
  }

  public String getScopeName() {
    return myScopeName;
  }

  public void setScopeName(String scopeName) {
    myScopeName = scopeName;
  }

  public String getColorName() {
    return myColorName;
  }

  public void setColorName(final String colorName) {
    myColorName = colorName;
  }

  public boolean isValid() {
    if (myScopeName == null || myScopeName.length() == 0) {
      return false;
    }

    if (myColorName == null) {
      return false;
    }

    return true;
  }

  public void save(@NotNull final Element e) {
    if (!isValid()) {
      return;
    }

    final Element tab = new Element(FileColorsModel.FILE_COLOR);

    tab.setAttribute(SCOPE_NAME, getScopeName());
    tab.setAttribute(COLOR, myColorName);

    e.addContent(tab);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FileColorConfiguration that = (FileColorConfiguration)o;

    if (!myColorName.equals(that.myColorName)) return false;
    if (!myScopeName.equals(that.myScopeName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myScopeName.hashCode();
    result = 31 * result + myColorName.hashCode();
    return result;
  }

  public FileColorConfiguration clone() throws CloneNotSupportedException {
    final FileColorConfiguration result = new FileColorConfiguration();

    result.myColorName = myColorName;
    result.myScopeName = myScopeName;

    return result;
  }

  @Nullable
  public static FileColorConfiguration load(@NotNull final Element e) {
    final String path = e.getAttributeValue(SCOPE_NAME);
    if (path == null) {
      return null;
    }

    final String colorName = e.getAttributeValue(COLOR);
    if (colorName == null) {
      return null;
    }

    return new FileColorConfiguration(path, colorName);
  }
}