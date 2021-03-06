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
package com.intellij.execution.console;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.DataManager;
import com.intellij.ide.impl.TypeSafeDataProviderAdapter;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.impl.DelegateColorScheme;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorFactoryImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiDocumentManagerImpl;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.SideBorder;
import com.intellij.util.FileContentUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.FocusManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Gregory.Shrago
 */
public class LanguageConsoleImpl implements Disposable, TypeSafeDataProvider {
  private static final int SEPARATOR_THICKNESS = 1;

  private final Project myProject;

  private final EditorEx myConsoleEditor;
  private final EditorEx myHistoryViewer;
  private final Document myEditorDocument;
  protected PsiFile myFile;

  private final JPanel myPanel = new JPanel(new BorderLayout());

  private String myTitle;
  private String myPrompt = "> ";
  private final LightVirtualFile myHistoryFile;

  private Editor myCurrentEditor;

  private final AtomicBoolean myForceScrollToEnd = new AtomicBoolean(false);
  private final MergingUpdateQueue myUpdateQueue;
  private Runnable myUiUpdateRunnable;

  private Editor myFullEditor;
  private ActionGroup myFullEditorActions;

  public LanguageConsoleImpl(final Project project, String title, final Language language) {
    myProject = project;
    myTitle = title;
    installEditorFactoryListener();
    final EditorFactory editorFactory = EditorFactory.getInstance();
    myHistoryFile = new LightVirtualFile(getTitle() + ".history.txt", StdFileTypes.PLAIN_TEXT, "");
    myEditorDocument = editorFactory.createDocument("");
    setLanguage(language);
    myConsoleEditor = (EditorEx)editorFactory.createEditor(myEditorDocument, myProject);

    myCurrentEditor = myConsoleEditor;
    myHistoryViewer = (EditorEx)editorFactory.createViewer(((EditorFactoryImpl)editorFactory).createDocument(true), myProject);
    final EditorColorsScheme colorsScheme = myConsoleEditor.getColorsScheme();
    final DelegateColorScheme scheme = new DelegateColorScheme(colorsScheme) {
      @Override
      public Color getDefaultBackground() {
        final Color color = getColor(ConsoleViewContentType.CONSOLE_BACKGROUND_KEY);
        return color == null ? super.getDefaultBackground() : color;
      }
    };
    myConsoleEditor.setColorsScheme(scheme);
    myHistoryViewer.setColorsScheme(scheme);
    myPanel.add(myHistoryViewer.getComponent(), BorderLayout.NORTH);
    myPanel.add(myConsoleEditor.getComponent(), BorderLayout.CENTER);
    setupComponents();
    myPanel.putClientProperty(DataManager.CLIENT_PROPERTY_DATA_PROVIDER, new TypeSafeDataProviderAdapter(this));
    myUpdateQueue = new MergingUpdateQueue("ConsoleUpdateQueue", 300, true, null);
    Disposer.register(this, myUpdateQueue);
    myPanel.addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        try {
          myHistoryViewer.getScrollingModel().disableAnimation();
          updateSizes(true);
        }
        finally {
          myHistoryViewer.getScrollingModel().enableAnimation();
        }
      }

      public void componentShown(ComponentEvent e) {
        componentResized(e);
      }
    });
  }

  public void setFullEditorMode(boolean fullEditorMode) {
    if (myFullEditor != null == fullEditorMode) return;
    final VirtualFile virtualFile = myFile.getVirtualFile();
    assert virtualFile != null;
    final FileEditorManagerEx fileManager = FileEditorManagerEx.getInstanceEx(getProject());
    if (!fullEditorMode) {
      fileManager.closeFile(virtualFile);
      myFullEditor = null;
      myPanel.removeAll();
      myPanel.add(myHistoryViewer.getComponent(), BorderLayout.NORTH);
      myPanel.add(myConsoleEditor.getComponent(), BorderLayout.CENTER);

      myHistoryViewer.setHorizontalScrollbarVisible(false);
    }
    else {
      myPanel.removeAll();
      myPanel.add(myHistoryViewer.getComponent(), BorderLayout.CENTER);
      myFullEditor = fileManager.openTextEditor(new OpenFileDescriptor(getProject(), virtualFile, 0), true);
      configureFullEditor();
      setConsoleFilePinned(fileManager);

      myHistoryViewer.setHorizontalScrollbarVisible(true);
    }
  }

  private void setConsoleFilePinned(FileEditorManagerEx fileManager) {
    if (myFullEditor == null) return;
    EditorWindow editorWindow = EditorWindow.DATA_KEY.getData(DataManager.getInstance().getDataContext(myFullEditor.getComponent()));
    if (editorWindow == null) {
      editorWindow = fileManager.getCurrentWindow();
    }
    if (editorWindow != null) {
      editorWindow.setFilePinned(myFile.getVirtualFile(), true);
    }
  }

  public void setFullEditorActions(ActionGroup actionGroup) {
    myFullEditorActions = actionGroup;
    configureFullEditor();
  }

  private void setupComponents() {
    setupEditorDefault(myConsoleEditor);
    setupEditorDefault(myHistoryViewer);
    setPrompt(myPrompt);
    myConsoleEditor.addEditorMouseListener(EditorActionUtil.createEditorPopupHandler(IdeActions.GROUP_CUT_COPY_PASTE));
    if (SEPARATOR_THICKNESS > 0) {
      myHistoryViewer.getComponent().setBorder(new SideBorder(Color.LIGHT_GRAY, SideBorder.BOTTOM));
    }
    myHistoryViewer.getComponent().setMinimumSize(new Dimension(0, 0));
    myHistoryViewer.getComponent().setPreferredSize(new Dimension(0, 0));
    myConsoleEditor.getSettings().setAdditionalLinesCount(2);
    myConsoleEditor.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(myProject, myFile.getVirtualFile()));
    myHistoryViewer.setCaretEnabled(false);
    myConsoleEditor.setHorizontalScrollbarVisible(true);
    final VisibleAreaListener areaListener = new VisibleAreaListener() {
      public void visibleAreaChanged(VisibleAreaEvent e) {
        final int offset = myConsoleEditor.getScrollingModel().getHorizontalScrollOffset();
        final ScrollingModel model = myHistoryViewer.getScrollingModel();
        final int historyOffset = model.getHorizontalScrollOffset();
        if (historyOffset != offset) {
          try {
            model.disableAnimation();
            model.scrollHorizontally(offset);
          }
          finally {
            model.enableAnimation();
          }
        }
      }
    };
    myConsoleEditor.getScrollingModel().addVisibleAreaListener(areaListener);
    final DocumentAdapter docListener = new DocumentAdapter() {
      @Override
      public void documentChanged(final DocumentEvent e) {
        queueUiUpdate(false);
      }
    };
    myEditorDocument.addDocumentListener(docListener, this);
    myHistoryViewer.getDocument().addDocumentListener(docListener, this);

    myHistoryViewer.getContentComponent().addKeyListener(new KeyAdapter() {
      public void keyTyped(KeyEvent event) {
        if (myFullEditor == null && UIUtil.isReallyTypedEvent(event)) {
          myConsoleEditor.getContentComponent().requestFocus();
          myConsoleEditor.processKeyTyped(event);
        }
      }
    });
    for (AnAction action : createActions()) {
      action.registerCustomShortcutSet(action.getShortcutSet(), myConsoleEditor.getComponent());
    }
    registerActionShortcuts(myHistoryViewer.getComponent());
  }

  protected AnAction[] createActions() {
    return AnAction.EMPTY_ARRAY;
  }

  private static void setupEditorDefault(EditorEx editor) {
    editor.getContentComponent().setFocusCycleRoot(false);
    editor.setHorizontalScrollbarVisible(false);
    editor.setVerticalScrollbarVisible(true);
    editor.getColorsScheme().setColor(EditorColors.CARET_ROW_COLOR, null);
    editor.setBorder(null);
    editor.getContentComponent().setFocusCycleRoot(false);

    final EditorSettings editorSettings = editor.getSettings();
    editorSettings.setAdditionalLinesCount(0);
    editorSettings.setAdditionalColumnsCount(1);
    editorSettings.setRightMarginShown(false);
    editorSettings.setFoldingOutlineShown(true);
    editorSettings.setLineNumbersShown(false);
    editorSettings.setLineMarkerAreaShown(false);
    editorSettings.setIndentGuidesShown(false);
    editorSettings.setVirtualSpace(false);
    editorSettings.setLineCursorWidth(1);
  }

  public void setUiUpdateRunnable(Runnable uiUpdateRunnable) {
    assert myUiUpdateRunnable == null : "can be set only once";
    myUiUpdateRunnable = uiUpdateRunnable;
  }

  public void flushAllUiUpdates() {
    myUpdateQueue.flush();
  }

  public LightVirtualFile getHistoryFile() {
    return myHistoryFile;
  }

  public String getPrompt() {
    return myPrompt;
  }

  public void setPrompt(String prompt) {
    myPrompt = prompt;
    ((EditorImpl)myConsoleEditor).setPrefixTextAndAttributes(myPrompt, ConsoleViewContentType.USER_INPUT.getAttributes());
  }

  public PsiFile getFile() {
    return myFile;
  }

  public EditorEx getHistoryViewer() {
    return myHistoryViewer;
  }

  public Document getEditorDocument() {
    return myEditorDocument;
  }

  public EditorEx getConsoleEditor() {
    return myConsoleEditor;
  }

  public Project getProject() {
    return myProject;
  }

  public String getTitle() {
    return myTitle;
  }

  public void setTitle(String title) {
    this.myTitle = title;
  }

  public void addToHistory(final String text, final TextAttributes attributes) {
    printToHistory(text, attributes);
  }

  public Editor getFullEditor() {
    return myFullEditor;
  }

  public void printToHistory(String text, final TextAttributes attributes) {
    text = StringUtil.convertLineSeparators(text);
    final boolean scrollToEnd = shouldScrollHistoryToEnd();
    final Document history = myHistoryViewer.getDocument();
    final MarkupModel markupModel = history.getMarkupModel(myProject);
    final int offset = history.getTextLength();
    appendToHistoryDocument(history, text);
    markupModel.addRangeHighlighter(offset,
                                    history.getTextLength(),
                                    HighlighterLayer.SYNTAX,
                                    attributes,
                                    HighlighterTargetArea.EXACT_RANGE);
    queueUiUpdate(scrollToEnd);
  }

  public String addCurrentToHistory(final TextRange textRange, final boolean erase, final boolean preserveMarkup) {
    final Ref<String> ref = Ref.create("");
    final boolean scrollToEnd = shouldScrollHistoryToEnd();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        ref.set(addTextRangeToHistory(textRange, myConsoleEditor, preserveMarkup));
        if (erase) {
          myConsoleEditor.getDocument().deleteString(textRange.getStartOffset(), textRange.getEndOffset());
        }
      }
    });
    queueUiUpdate(scrollToEnd);
    return ref.get();
  }

  public boolean shouldScrollHistoryToEnd() {
    return myHistoryViewer.getCaretModel().getOffset() == myHistoryViewer.getDocument().getTextLength();
  }

  private void scrollHistoryToEnd() {
    final int lineCount = myHistoryViewer.getDocument().getLineCount();
    if (lineCount == 0) return;
    myHistoryViewer.getCaretModel().moveToOffset(myHistoryViewer.getDocument().getLineStartOffset(lineCount - 1), false);
    myHistoryViewer.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
  }

  private String addTextRangeToHistory(TextRange textRange, final EditorEx consoleEditor, boolean preserveMarkup) {
    final DocumentImpl history = (DocumentImpl)myHistoryViewer.getDocument();
    final MarkupModel markupModel = history.getMarkupModel(myProject);
    appendToHistoryDocument(history, myPrompt);
    markupModel.addRangeHighlighter(history.getTextLength() - myPrompt.length(), history.getTextLength(), HighlighterLayer.SYNTAX,
                                    ConsoleViewContentType.USER_INPUT.getAttributes(),
                                    HighlighterTargetArea.EXACT_RANGE);

    final String text = consoleEditor.getDocument().getText(textRange);
     //offset can be changed after text trimming after insert due to buffer constraints
    appendToHistoryDocument(history, text);
    int offset = history.getTextLength() - text.length();
    final int localStartOffset = textRange.getStartOffset();
    final HighlighterIterator iterator = consoleEditor.getHighlighter().createIterator(localStartOffset);
    final int localEndOffset = textRange.getEndOffset();
    while (!iterator.atEnd()) {
      final int itStart = iterator.getStart();
      if (itStart > localEndOffset) break;
      final int itEnd = iterator.getEnd();
      if (itEnd >= localStartOffset) {
        final int start = Math.max(itStart, localStartOffset) - localStartOffset + offset;
        final int end = Math.min(itEnd, localEndOffset) - localStartOffset + offset;
        markupModel.addRangeHighlighter(start, end, HighlighterLayer.SYNTAX, iterator.getTextAttributes(),
                                        HighlighterTargetArea.EXACT_RANGE);
      }
      iterator.advance();
    }
    if (preserveMarkup) {
      duplicateHighlighters(markupModel, consoleEditor.getDocument().getMarkupModel(myProject), offset, textRange);
      duplicateHighlighters(markupModel, consoleEditor.getMarkupModel(), offset, textRange);
    }
    if (!text.endsWith("\n")) {
      appendToHistoryDocument(history, "\n");
    }
    return text;
  }

  protected void appendToHistoryDocument(@NotNull Document history, @NotNull String text) {
    history.insertString(history.getTextLength(), text);
  }

  private static void duplicateHighlighters(MarkupModel to, MarkupModel from, int offset, TextRange textRange) {
    for (RangeHighlighter rangeHighlighter : from.getAllHighlighters()) {
      final int localOffset = textRange.getStartOffset();
      final int start = Math.max(rangeHighlighter.getStartOffset(), localOffset) - localOffset;
      final int end = Math.min(rangeHighlighter.getEndOffset(), textRange.getEndOffset()) - localOffset;
      if (start > end) continue;
      final RangeHighlighter h = to.addRangeHighlighter(
        start + offset, end + offset, rangeHighlighter.getLayer(), rangeHighlighter.getTextAttributes(), rangeHighlighter.getTargetArea());
      ((RangeHighlighterEx)h).setAfterEndOfLine(((RangeHighlighterEx)rangeHighlighter).isAfterEndOfLine());
    }
  }

  public JComponent getComponent() {
    return myPanel;
  }

  public void queueUiUpdate(final boolean forceScrollToEnd) {
    myForceScrollToEnd.compareAndSet(false, forceScrollToEnd);
    myUpdateQueue.queue(new Update("UpdateUi") {
      public void run() {
        if (Disposer.isDisposed(LanguageConsoleImpl.this)) return;
        updateSizes(myForceScrollToEnd.getAndSet(false));
        if (myUiUpdateRunnable != null) {
          ApplicationManager.getApplication().runReadAction(myUiUpdateRunnable);
        }
      }
    });
  }

  private void updateSizes(boolean forceScrollToEnd) {
    if (myFullEditor != null) return;
    final Dimension panelSize = myPanel.getSize();
    final Dimension historyContentSize = myHistoryViewer.getContentSize();
    final Dimension contentSize = myConsoleEditor.getContentSize();
    final Dimension newEditorSize = new Dimension();
    final int minHistorySize = historyContentSize.height > 0 ? 2 * myHistoryViewer.getLineHeight() + SEPARATOR_THICKNESS : 0;
    final int width = Math.max(contentSize.width, historyContentSize.width);
    newEditorSize.height = Math.min(Math.max(panelSize.height - minHistorySize, 2 * myConsoleEditor.getLineHeight()),
                                    contentSize.height + myConsoleEditor.getScrollPane().getHorizontalScrollBar().getHeight());
    newEditorSize.width = width + myConsoleEditor.getScrollPane().getHorizontalScrollBar().getHeight();
    myConsoleEditor.getSettings()
      .setAdditionalColumnsCount(2 + (width - contentSize.width) / EditorUtil.getSpaceWidth(Font.PLAIN, myConsoleEditor));
    myHistoryViewer.getSettings()
      .setAdditionalColumnsCount(2 + (width - historyContentSize.width) / EditorUtil.getSpaceWidth(Font.PLAIN, myHistoryViewer));

    final Dimension editorSize = myConsoleEditor.getComponent().getSize();
    if (!editorSize.equals(newEditorSize)) {
      myConsoleEditor.getComponent().setPreferredSize(newEditorSize);
    }
    final boolean scrollToEnd = forceScrollToEnd || shouldScrollHistoryToEnd();
    final Dimension newHistorySize = new Dimension(
      width, Math.max(0, Math.min(minHistorySize == 0 ? 0 : historyContentSize.height + SEPARATOR_THICKNESS,
                                  panelSize.height - newEditorSize.height)));
    final Dimension historySize = myHistoryViewer.getComponent().getSize();
    if (!historySize.equals(newHistorySize)) {
      myHistoryViewer.getComponent().setPreferredSize(newHistorySize);
    }
    myPanel.validate();
    if (scrollToEnd) scrollHistoryToEnd();
  }

  public void dispose() {
    final EditorFactory editorFactory = EditorFactory.getInstance();
    editorFactory.releaseEditor(myConsoleEditor);
    editorFactory.releaseEditor(myHistoryViewer);

    final VirtualFile virtualFile = myFile.getVirtualFile();
    assert virtualFile != null;
    final FileEditorManager editorManager = FileEditorManager.getInstance(getProject());
    final boolean isOpen = editorManager.isFileOpen(virtualFile);
    if (isOpen) {
      editorManager.closeFile(virtualFile);
    }
  }

  public void calcData(DataKey key, DataSink sink) {
    if (OpenFileDescriptor.NAVIGATE_IN_EDITOR == key) {
      sink.put(OpenFileDescriptor.NAVIGATE_IN_EDITOR, myConsoleEditor);
      return;
    }
    final Object o =
      ((FileEditorManagerImpl)FileEditorManager.getInstance(getProject())).getData(key.getName(), myConsoleEditor, myFile.getVirtualFile());
    sink.put(key, o);
  }

  private void installEditorFactoryListener() {
    myProject.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerAdapter() {
      @Override
      public void fileOpened(FileEditorManager source, VirtualFile file) {
        if (file != myFile.getVirtualFile()) return;
        if (myConsoleEditor != null) {
          queueUiUpdate(false);
          for (FileEditor fileEditor : source.getAllEditors()) {
            if (!(fileEditor instanceof TextEditor)) continue;
            final Editor editor = ((TextEditor)fileEditor).getEditor();
            registerActionShortcuts(editor.getComponent());
            editor.getCaretModel().addCaretListener(new CaretListener() {
              public void caretPositionChanged(CaretEvent e) {
                queueUiUpdate(false);
              }
            });
            editor.getContentComponent().addFocusListener(new FocusListener() {
              public void focusGained(final FocusEvent e) {
                myCurrentEditor = editor;
              }

              public void focusLost(final FocusEvent e) {
              }
            });
          }
        }
      }

      @Override
      public void fileClosed(FileEditorManager source, VirtualFile file) {
        if (file != myFile.getVirtualFile()) return;
        if (myUiUpdateRunnable != null) {
          ApplicationManager.getApplication().runReadAction(myUiUpdateRunnable);
        }
      }
    });
  }

  protected void registerActionShortcuts(JComponent component) {
    final ArrayList<AnAction> actionList =
      (ArrayList<AnAction>)myConsoleEditor.getComponent().getClientProperty(AnAction.ourClientProperty);
    if (actionList != null) {
      for (AnAction anAction : actionList) {
        anAction.registerCustomShortcutSet(anAction.getShortcutSet(), component);
      }
    }
  }

  public Editor getCurrentEditor() {
    return myCurrentEditor;
  }

  public void setLanguage(Language language) {
    final PsiFile prevFile = myFile;
    if (prevFile != null) {
      final VirtualFile file = prevFile.getVirtualFile();
      assert file instanceof LightVirtualFile;
      ((LightVirtualFile)file).setValid(false);
      ((PsiManagerEx)prevFile.getManager()).getFileManager().setViewProvider(file, null);
    }

    final FileType type = language.getAssociatedFileType();
    @NonNls final String name = getTitle();
    final LightVirtualFile newVFile = new LightVirtualFile(name, language, myEditorDocument.getText());
    FileDocumentManagerImpl.registerDocument(myEditorDocument, newVFile);
    myFile = ((PsiFileFactoryImpl)PsiFileFactory.getInstance(myProject)).trySetupPsiForFile(newVFile, language, true, false);
    if (myFile == null) {
      throw new AssertionError("file=null, name=" + name + ", language=" + language.getDisplayName());
    }
    PsiDocumentManagerImpl.cachePsi(myEditorDocument, myFile);
    FileContentUtil.reparseFiles(myProject, Collections.<VirtualFile>singletonList(newVFile), false);

    if (prevFile != null) {
      final FileEditorManagerEx editorManager = FileEditorManagerEx.getInstanceEx(getProject());
      final VirtualFile file = prevFile.getVirtualFile();
      if (file != null && myFullEditor != null) {
        myFullEditor = null;
        final FileEditor prevEditor = editorManager.getSelectedEditor(file);
        final boolean focusEditor;
        final int offset;
        if (prevEditor != null) {
          offset = prevEditor instanceof TextEditor ? ((TextEditor)prevEditor).getEditor().getCaretModel().getOffset() : 0;
          final Component owner = FocusManager.getCurrentManager().getFocusOwner();
          focusEditor = owner != null && SwingUtilities.isDescendingFrom(owner, prevEditor.getComponent());
        }
        else {
          focusEditor = false;
          offset = 0;
        }
        editorManager.closeFile(file);
        myFullEditor = editorManager.openTextEditor(new OpenFileDescriptor(getProject(), newVFile, offset), focusEditor);
        configureFullEditor();
        setConsoleFilePinned(editorManager);
      }
    }
  }

  private void configureFullEditor() {
    if (myFullEditor == null || myFullEditorActions == null) return;
    final JPanel header = new JPanel(new BorderLayout());
    header.add(ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, myFullEditorActions, true).getComponent(),
               BorderLayout.EAST);
    myFullEditor.setHeaderComponent(header);
    myFullEditor.getSettings().setLineMarkerAreaShown(false);
  }

  public void setInputText(final String query) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        myConsoleEditor.getDocument().setText(query);
      }
    });
  }

  public static void printToConsole(final LanguageConsoleImpl console,
                                    final String string,
                                    final ConsoleViewContentType mainType,
                                    ConsoleViewContentType additionalType) {
    final TextAttributes mainAttributes = mainType.getAttributes();
    final TextAttributes attributes;
    if (additionalType == null) {
      attributes = mainAttributes;
    }
    else {
      attributes = additionalType.getAttributes().clone();
      attributes.setBackgroundColor(mainAttributes.getBackgroundColor());
    }
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        console.printToHistory(string, attributes);
      }
    }, ModalityState.stateForComponent(console.getComponent()));
  }
}
