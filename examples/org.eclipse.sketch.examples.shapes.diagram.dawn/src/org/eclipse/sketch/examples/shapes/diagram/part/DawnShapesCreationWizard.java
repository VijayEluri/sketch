package org.eclipse.sketch.examples.shapes.diagram.part;

import org.eclipse.emf.cdo.dawn.preferences.PreferenceConstants;
import org.eclipse.emf.cdo.dawn.ui.wizards.DawnCreateNewDiagramResourceWizardPage;
import org.eclipse.emf.cdo.dawn.ui.wizards.DawnCreateNewResourceWizardPage;
import org.eclipse.emf.cdo.dawn.util.connection.CDOConnectionUtil;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.view.CDOView;

import org.eclipse.emf.common.util.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import java.lang.reflect.InvocationTargetException;

public class DawnShapesCreationWizard extends ShapesCreationWizard {
	private CDOView view;

	private DawnCreateNewDiagramResourceWizardPage dawnDiagramModelFilePage;

	private DawnCreateNewResourceWizardPage dawnDomainModelFilePage;

	public DawnShapesCreationWizard() {
		super();
		CDOConnectionUtil.instance.init(
				PreferenceConstants.getRepositoryName(),
				PreferenceConstants.getProtocol(),
				PreferenceConstants.getServerName());
		CDOSession session = CDOConnectionUtil.instance.openSession();
		view = CDOConnectionUtil.instance.openView(session);
	}

	@Override
	public boolean performFinish() {
		IRunnableWithProgress op = new WorkspaceModifyOperation(null) {
			@Override
			protected void execute(IProgressMonitor monitor)
					throws CoreException, InterruptedException {
				URI diagramResourceURI = dawnDiagramModelFilePage.getURI();
				URI domainModelResourceURI = dawnDomainModelFilePage.getURI();

				diagram = DawnShapesDiagramEditorUtil.createDiagram(
						diagramResourceURI, domainModelResourceURI, monitor);

				if (isOpenNewlyCreatedDiagramEditor() && diagram != null) {
					try {
						DawnShapesDiagramEditorUtil.openDiagram(diagram);
					} catch (PartInitException e) {
						ErrorDialog.openError(getContainer().getShell(),
								Messages.ShapesCreationWizardOpenEditorError,
								null, e.getStatus());
					}
				}
			}
		};
		try {
			getContainer().run(false, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof CoreException) {
				ErrorDialog.openError(getContainer().getShell(),
						Messages.ShapesCreationWizardCreationError, null,
						((CoreException) e.getTargetException()).getStatus());
			} else {
				ShapesDiagramEditorPlugin.getInstance().logError(
						"Error creating diagram", e.getTargetException()); //$NON-NLS-1$
			}
			return false;
		}
		return diagram != null;
	}

	@Override
	public void addPages() {

		dawnDiagramModelFilePage = new DawnCreateNewDiagramResourceWizardPage(
				"shapes", false, view);
		dawnDiagramModelFilePage
				.setTitle(Messages.ShapesCreationWizard_DiagramModelFilePageTitle);
		dawnDiagramModelFilePage
				.setDescription(Messages.ShapesCreationWizard_DiagramModelFilePageDescription);
		dawnDiagramModelFilePage.setCreateAutomaticResourceName(true);
		addPage(dawnDiagramModelFilePage);

		dawnDomainModelFilePage = new DawnCreateNewResourceWizardPage("shapes",
				true, view) {
			@Override
			public void setVisible(boolean visible) {
				if (visible) {
					URI uri = dawnDiagramModelFilePage.getURI();
					String fileName = uri.lastSegment();
					fileName = fileName.substring(0, fileName.length()
							- ".shapes".length()); //$NON-NLS-1$
					fileName += ".shapes";
					dawnDomainModelFilePage.setResourceNamePrefix(fileName);
					dawnDomainModelFilePage
							.setResourcePath(dawnDiagramModelFilePage
									.getResourcePath());
				}
				super.setVisible(visible);
			}
		};
		//dawnDomainModelFilePage
		//		.setTitle(Messages.ShapesCreationWizard_DomainModelFilePageTitle);
		//dawnDomainModelFilePage
		//		.setDescription(Messages.ShapesCreationWizard_DomainModelFilePageDescription);

		dawnDomainModelFilePage
				.setResourceValidationType(DawnCreateNewResourceWizardPage.VALIDATION_WARN);
		addPage(dawnDomainModelFilePage);
	}

	@Override
	public void dispose() {
		view.close();
	}
}
