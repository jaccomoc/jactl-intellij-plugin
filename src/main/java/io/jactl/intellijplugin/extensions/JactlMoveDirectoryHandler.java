package io.jactl.intellijplugin.extensions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JactlMoveDirectoryHandler extends MoveFilesOrDirectoriesHandler {
  @Override
  public boolean canMove(PsiElement[] elements, PsiElement targetContainer, @Nullable PsiReference reference) {
    return super.canMove(elements, targetContainer, reference);
  }

  @Override
  public boolean isValidTarget(PsiElement targetElement, PsiElement[] sources) {
    return super.isValidTarget(targetElement, sources);
  }

  @Override
  public void doMove(PsiElement[] elements, PsiElement targetContainer) {
    super.doMove(elements, targetContainer);
  }

  @Override
  public PsiElement @Nullable [] adjustForMove(Project project, PsiElement[] sourceElements, PsiElement targetElement) {
    return super.adjustForMove(project, sourceElements, targetElement);
  }

  @Override
  public void doMove(Project project, PsiElement[] elements, PsiElement targetContainer, @Nullable MoveCallback callback) {
    super.doMove(project, elements, targetContainer, callback);
  }

  @Override
  public boolean tryToMove(PsiElement element, Project project, DataContext dataContext, PsiReference reference, Editor editor) {
    return super.tryToMove(element, project, dataContext, reference, editor);
  }

  @Override
  public @Nullable String getActionName(PsiElement @NotNull [] elements) {
    return super.getActionName(elements);
  }
}
