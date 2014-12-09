/*
 * Copyright 2013 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.kie.workbench.drools.client.perspectives;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.guvnor.common.services.project.builder.model.BuildResults;
import org.guvnor.common.services.project.context.ProjectContextChangeEvent;
import org.guvnor.common.services.project.model.Package;
import org.guvnor.common.services.project.model.Project;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.kie.workbench.common.screens.explorer.client.utils.Utils;
import org.kie.workbench.common.screens.explorer.client.widgets.BaseViewPresenter;
import org.kie.workbench.common.screens.explorer.client.widgets.View;
import org.kie.workbench.common.screens.explorer.client.widgets.business.BusinessViewWidget;
import org.kie.workbench.common.screens.explorer.model.FolderItem;
import org.kie.workbench.common.screens.explorer.model.FolderItemType;
import org.kie.workbench.common.screens.explorer.model.ProjectExplorerContent;
import org.kie.workbench.common.screens.explorer.service.Option;
import org.kie.workbench.common.widgets.client.callbacks.DefaultErrorCallback;
import org.kie.workbench.common.widgets.client.callbacks.HasBusyIndicatorDefaultErrorCallback;
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;
import org.uberfire.backend.organizationalunit.OrganizationalUnit;
import org.uberfire.backend.repositories.Repository;

/**
 * Repository, Package, Folder and File explorer
 */
@ApplicationScoped
public class BusinessViewPresenterImpl extends BaseViewPresenter {

	static {
		System.out.println("BusinessViewPresenterImpl");
	}

	@Inject
	protected BusinessViewWidget view;

	private Set<Option> options = new HashSet<Option>(Arrays.asList(
			Option.BUSINESS_CONTENT, Option.TREE_NAVIGATOR,
			Option.EXCLUDE_HIDDEN_ITEMS));

	@Override
	protected void setOptions(Set<Option> options) {
		this.options = new HashSet<Option>(options);
	}

	native void consoleLog(String message) /*-{
											console.log("BusinessViewPresenterImpl: " + message);
											}-*/;

	public BusinessViewPresenterImpl() {
		consoleLog("BusinessViewPresenterImpl:: constructor");
	}
	
	@Override
	public Set<Option> getActiveOptions() {
		return options;
	}

	@Override
	protected View getView() {
		return view;
	}

	@Override
	public void initialiseViewForActiveContext(
			final OrganizationalUnit organizationalUnit,
			final Repository repository, final Project project) {
		
		consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: start");
		
		doInitialiseViewForActiveContext(organizationalUnit, repository,
				project, null, null, true);
		
		consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: end");
	}

	private void doInitialiseViewForActiveContext(
			final OrganizationalUnit organizationalUnit,
			final Repository repository, final Project project,
			final Package pkg, final FolderItem folderItem,
			final boolean showLoadingIndicator) {

		consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: doInitialiseViewForActiveContext1");
		if (showLoadingIndicator) {
			getView().showBusyIndicator(CommonConstants.INSTANCE.Loading());
		}

		explorerService.call(new RemoteCallback<ProjectExplorerContent>() {
			@Override
			public void callback(final ProjectExplorerContent content) {
				consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: doInitialiseViewForActiveContext2");
				
				boolean signalChange = false;
				boolean buildSelectedProject = false;

				if (Utils.hasOrganizationalUnitChanged(
						content.getOrganizationalUnit(),
						activeOrganizationalUnit)) {
					signalChange = true;
					activeOrganizationalUnit = content.getOrganizationalUnit();
					consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: doInitialiseViewForActiveContext3");
				}
				if (Utils.hasRepositoryChanged(content.getRepository(),
						activeRepository)) {
					signalChange = true;
					activeRepository = content.getRepository();
					consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: doInitialiseViewForActiveContext4");
				}
				if (Utils.hasProjectChanged(content.getProject(), activeProject)) {
					signalChange = true;
					buildSelectedProject = true;
					activeProject = content.getProject();
					consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: doInitialiseViewForActiveContext5");
				}
				if (Utils.hasFolderItemChanged(content.getFolderListing()
						.getItem(), activeFolderItem)) {
					signalChange = true;
					activeFolderItem = content.getFolderListing().getItem();
					if (activeFolderItem != null
							&& activeFolderItem.getItem() != null
							&& activeFolderItem.getItem() instanceof Package) {
						consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: doInitialiseViewForActiveContext6");
						activePackage = (Package) activeFolderItem.getItem();
					} else if (activeFolderItem == null
							|| activeFolderItem.getItem() == null) {
						activePackage = null;
						consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: doInitialiseViewForActiveContext7");
					}
				}

				if (signalChange) {
					fireContextChangeEvent();
				}

				if (buildSelectedProject) {
					buildProject(activeProject);
				}

				activeContent = content.getFolderListing();

				getView().getExplorer().clear();
				getView().setContent(content.getOrganizationalUnits(),
						activeOrganizationalUnit, content.getRepositories(),
						activeRepository, content.getProjects(), activeProject,
						content.getFolderListing(), content.getSiblings());

				getView().hideBusyIndicator();
			}

		}, new HasBusyIndicatorDefaultErrorCallback(getView())).getContent(
				organizationalUnit, repository, project, pkg, folderItem,
				getActiveOptions());
	}

	private void fireContextChangeEvent() {
		if (activeFolderItem == null) {
			contextChangedEvent.fire(new ProjectContextChangeEvent(
					activeOrganizationalUnit, activeRepository, activeProject));
			
			consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: fireContextChangeEvent1");
			
			return;
		}

		if (activeFolderItem.getItem() instanceof Package) {
			activePackage = (Package) activeFolderItem.getItem();
			contextChangedEvent.fire(new ProjectContextChangeEvent(
					activeOrganizationalUnit, activeRepository, activeProject,
					activePackage));
			
			consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: fireContextChangeEvent2");
		} else if (activeFolderItem.getType().equals(FolderItemType.FOLDER)) {
			explorerService.call(new RemoteCallback<Package>() {
				@Override
				public void callback(final Package pkg) {
					if (Utils.hasPackageChanged(pkg, activePackage)) {
						activePackage = pkg;
						contextChangedEvent.fire(new ProjectContextChangeEvent(
								activeOrganizationalUnit, activeRepository,
								activeProject, activePackage));
						consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: fireContextChangeEvent3");
					} else {
						contextChangedEvent.fire(new ProjectContextChangeEvent(
								activeOrganizationalUnit, activeRepository,
								activeProject));
						consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: fireContextChangeEvent4");
					}
				}
			}).resolvePackage(activeFolderItem);
		}
	}

	private void buildProject(final Project project) {
		if (project == null) {
			consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: buildProject1");
			return;
		}
		buildService.call(new RemoteCallback<BuildResults>() {
			@Override
			public void callback(final BuildResults results) {
				buildResultsEvent.fire(results);
				consoleLog("BusinessViewPresenterImpl:: initialiseViewForActiveContext:: buildProject2");
			}
		}, new DefaultErrorCallback()).build(project);
	}

}