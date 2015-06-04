package apgas.ui.buildpath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import apgas.ui.Initializer;

public class APGASContainerPage extends WizardPage implements
    IClasspathContainerPage, IClasspathContainerPageExtension {

  private Combo mProjectsCombo;
  private IProject mOwnerProject;
  private String mLibsProjectName;

  public APGASContainerPage() {
    super("APGASContainerPage");
    setTitle("APGAS Library");
    setDescription("APGAS Library");
  }

  @Override
  public void createControl(Composite parent) {
    final Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));

    final Label label = new Label(composite, SWT.NONE);
    label.setText("project:");

    final String[] projects = getAPGASProjects();

    this.mProjectsCombo = new Combo(composite, SWT.READ_ONLY);
    this.mProjectsCombo.setItems(projects);

    final int index;
    if (this.mOwnerProject != null) {
      index = indexOf(projects, this.mLibsProjectName);
    } else {
      if (this.mProjectsCombo.getItemCount() > 0) {
        index = 0;
      } else {
        index = -1;
      }
    }
    if (index != -1) {
      this.mProjectsCombo.select(index);
    }
    final GridData gd = new GridData();
    gd.grabExcessHorizontalSpace = true;
    gd.minimumWidth = 100;

    this.mProjectsCombo.setLayoutData(gd);

    setControl(composite);
  }

  private int indexOf(final String[] array, final String string) {
    return 0;
  }

  private String[] getAPGASProjects() {
    final String[] result = { "APGAS" };
    return result;
  }

  @Override
  public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
    this.mOwnerProject = (project == null ? null : project.getProject());
  }

  @Override
  public boolean finish() {
    return true;
  }

  @Override
  public IClasspathEntry getSelection() {
    IPath path = new Path(Initializer.APGAS_CONTAINER_ID);

    final int index = this.mProjectsCombo.getSelectionIndex();
    if (index != -1) {
      final String selectedProjectName = this.mProjectsCombo.getItem(index);

      if (this.mOwnerProject == null
          || !selectedProjectName.equals(this.mOwnerProject.getName())) {
        path = path.append(selectedProjectName);
      }
    }

    return JavaCore.newContainerEntry(path);
  }

  @Override
  public void setSelection(IClasspathEntry containerEntry) {
    final IPath path = (containerEntry == null ? null : containerEntry
        .getPath());

    if (path == null || path.segmentCount() == 1) {
      if (this.mOwnerProject != null) {
        this.mLibsProjectName = this.mOwnerProject.getName();
      }
    } else {
      this.mLibsProjectName = path.segment(1);
    }
  }

}
