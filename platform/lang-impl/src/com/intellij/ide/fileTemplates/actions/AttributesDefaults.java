package com.intellij.ide.fileTemplates.actions;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.Map;

/**
 * @author Roman Chernyatchik
*/
public class AttributesDefaults {
  private final String myDefaultName;
  private final TextRange myDefaultRange;
  private final Map<String, Pair<String, TextRange>> myNamesToValueAndRangeMap = new HashMap<String, Pair<String, TextRange>>();

  public AttributesDefaults(@NonNls @Nullable final String defaultName,
                            @Nullable final TextRange defaultRange) {
    myDefaultName = defaultName;
    myDefaultRange = defaultRange;
  }

  public AttributesDefaults(@NonNls @Nullable final String defaultName) {
    this(defaultName, null);
  }

  public AttributesDefaults() {
    this(null, null);
  }

  @Nullable
  public String getDefaultFileName() {
    return myDefaultName;
  }
  @Nullable
  public TextRange getDefaultFileNameSelection() {
    return myDefaultRange;
  }

  public void add(@NonNls @NotNull final String attributeKey,
                  @NonNls @NotNull final String value,
                  @Nullable final TextRange selectionRange) {
    myNamesToValueAndRangeMap.put(attributeKey, new Pair<String, TextRange>(value, selectionRange));
  }

  public void add(@NonNls @NotNull final String attributeKey,
                  @NonNls @NotNull final String value) {
    add(attributeKey, value, null);
  }

  @Nullable
  public TextRange getRangeFor(@NonNls @NotNull final String attributeKey) {
    final Pair<String, TextRange> valueAndRange = myNamesToValueAndRangeMap.get(attributeKey);
    return valueAndRange == null ? null : valueAndRange.second;
  }

  @Nullable
  public String getDefaultValueFor(@NonNls @NotNull final String attributeKey) {
    final Pair<String, TextRange> valueAndRange = myNamesToValueAndRangeMap.get(attributeKey);
    return valueAndRange == null ? null : valueAndRange.first;
  }
}