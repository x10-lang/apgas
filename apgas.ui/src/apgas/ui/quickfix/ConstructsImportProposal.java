package apgas.ui.quickfix;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.PlatformUI;

public class ConstructsImportProposal implements IJavaCompletionProposal {

  private final IJavaProject fJavaProject;
  private final ImportRewrite fImportRewrite;

  public ConstructsImportProposal(IJavaProject project, ImportRewrite rewrite) {
    fJavaProject = project;
    fImportRewrite = rewrite;
  }

  @Override
  public void apply(IDocument document) {
    try {
      PlatformUI.getWorkbench().getActiveWorkbenchWindow()
          .run(false, true, new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
              try {
                final Change change = createChange();
                change.initializeValidationData(new NullProgressMonitor());
                final PerformChangeOperation op = new PerformChangeOperation(
                    change);
                op.setUndoManager(RefactoringCore.getUndoManager(),
                    getDisplayString());
                op.setSchedulingRule(fJavaProject.getProject().getWorkspace()
                    .getRoot());
                op.run(monitor);
              } catch (final CoreException e) {
                throw new InvocationTargetException(e);
              } catch (final OperationCanceledException e) {
                throw new InterruptedException();
              }
            }
          });
    } catch (final InvocationTargetException e) {
      // fail silently
    } catch (final InterruptedException e) {
      // fail silently
    }
  }

  protected Change createChange() throws CoreException {
    final TextFileChange change = new TextFileChange("Add Import",
        (IFile) fImportRewrite.getCompilationUnit().getResource());
    change.setEdit(fImportRewrite.rewriteImports(new NullProgressMonitor()));
    return change;
  }

  @Override
  public String getAdditionalProposalInfo() {
    return "Add import for APGAS constructs";
  }

  @Override
  public IContextInformation getContextInformation() {
    return null;
  }

  @Override
  public String getDisplayString() {
    return "Add import for APGAS constructs";
  }

  @Override
  public Image getImage() {
    final ISharedImages images = JavaUI.getSharedImages();
    final Image image = images.getImage(ISharedImages.IMG_OBJS_IMPDECL);
    return image;
  }

  @Override
  public Point getSelection(IDocument arg0) {
    return null;
  }

  @Override
  public int getRelevance() {
    return 15;
  }

}
