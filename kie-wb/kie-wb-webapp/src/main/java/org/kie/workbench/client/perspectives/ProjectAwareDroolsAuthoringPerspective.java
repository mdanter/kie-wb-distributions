/*
 * Copyright 2013 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.workbench.client.perspectives;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.guvnor.common.services.project.context.ProjectContextChangeEvent;
import org.guvnor.common.services.project.model.Project;
import org.guvnor.common.services.project.service.ProjectService;
import org.guvnor.inbox.client.InboxPresenter;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.api.AfterInitialization;
import org.kie.workbench.client.resources.i18n.AppConstants;
import org.kie.workbench.common.screens.projecteditor.client.menu.ProjectMenu;
import org.kie.workbench.common.widgets.client.handlers.NewResourcePresenter;
import org.kie.workbench.common.widgets.client.handlers.NewResourcesMenu;
import org.kie.workbench.common.widgets.client.menu.RepositoryMenu;
import org.uberfire.backend.organizationalunit.OrganizationalUnit;
import org.uberfire.backend.organizationalunit.OrganizationalUnitService;
import org.uberfire.backend.repositories.Repository;
import org.uberfire.backend.repositories.RepositoryService;
import org.uberfire.backend.vfs.Path;
import org.uberfire.backend.vfs.VFSService;
import org.uberfire.client.annotations.Perspective;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPerspective;
import org.uberfire.client.annotations.WorkbenchToolBar;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.PanelDefinition;
import org.uberfire.workbench.model.PanelType;
import org.uberfire.workbench.model.PerspectiveDefinition;
import org.uberfire.workbench.model.Position;
import org.uberfire.workbench.model.impl.PanelDefinitionImpl;
import org.uberfire.workbench.model.impl.PartDefinitionImpl;
import org.uberfire.workbench.model.impl.PerspectiveDefinitionImpl;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.Menus;
import org.uberfire.workbench.model.toolbar.IconType;
import org.uberfire.workbench.model.toolbar.ToolBar;
import org.uberfire.workbench.model.toolbar.impl.DefaultToolBar;
import org.uberfire.workbench.model.toolbar.impl.DefaultToolBarItem;

import com.google.gwt.user.client.Window;

@ApplicationScoped
@WorkbenchPerspective(identifier = "projectAwareDroolsAuthoringPerspective")
public class ProjectAwareDroolsAuthoringPerspective {

	private AppConstants constants = AppConstants.INSTANCE;

	private String projectPathString;
	private String repoPathString;

	@Inject
	private NewResourcePresenter newResourcePresenter;

	@Inject
	private NewResourcesMenu newResourcesMenu;

	@Inject
	private ProjectMenu projectMenu;

	@Inject
	private PlaceManager placeManager;

	@Inject
	private RepositoryMenu repositoryMenu;

	@Inject
	protected Event<ProjectContextChangeEvent> contextChangedEvent;

	@Inject
	private Caller<VFSService> vfsServices;

	@Inject
	private Caller<ProjectService> projectServices;

	@Inject
	private Caller<RepositoryService> repoServices;

	@Inject
	private Caller<OrganizationalUnitService> orgUnitServices;

	private Menus menus;
	private ToolBar toolBar;

	private Path repoPath;
	private Path projectPath;
	private Project activeProject;
	private Repository activeRepository;
	
	private PerspectiveDefinition perspective;

	native void consoleLog(String message) /*-{
											console.log( "projectAwareDroolsAuthoringPerspective: " + message );
											}-*/;

	@AfterInitialization
	public void init() {

		buildPerspective();
		buildMenuBar();
		buildToolBar();

		// populate Project object from path

	}

	@WorkbenchMenu
	public Menus getMenus() {
		return this.menus;
	}

	@WorkbenchToolBar
	public ToolBar getToolBar() {
		return this.toolBar;
	}
	
	private void getProjectPath() {
		//gets the path param from a GET parameter and creates a Path object from it
		projectPathString = ((Window.Location.getParameterMap().containsKey("path")) ? Window.Location
				.getParameterMap().get("path").toString()
				: "");
 
 
		projectPathString = "git://master@uf-playground/mortgage";
		repoPathString = "git://master@uf-playground/";
 
		consoleLog("STRING projectPath via GET: " + projectPathString);
		consoleLog("STRING repoPath via GET: " + repoPathString);
 
		if (projectPathString != null && !projectPathString.isEmpty()) {
 
			vfsServices.call(new RemoteCallback<Path>() {
				@Override
				public void callback(final Path repoPath) {
					//populate Project object from path
		            buildProject(repoPath);
				}
			}).get(repoPathString);
		}
	}

	private void buildProject(final Path repoPath) {

		consoleLog("found Repository Path: "+repoPath.toString());
		
		repoServices.call(new RemoteCallback<Repository>() {
			 
			@Override
			public void callback(final Repository repository) {
				consoleLog("found the Repository with URI: " + repository.getUri());
				activeRepository = repository;
				
				orgUnitServices.call(new RemoteCallback<Collection<OrganizationalUnit>>() {
 
					@Override
					public void callback(final Collection<OrganizationalUnit> ous) {
						OrganizationalUnit aou = null;
						for(OrganizationalUnit ou : ous) {
							if(ou.getRepositories().contains(repository)){
								aou = ou;
								consoleLog("found specific OU that contains repository: "+ou.getName());
		                        break;
		                    }
						}
						if(aou!=null) {
							final OrganizationalUnit activeOrganizationalUnit = aou;
														
									vfsServices.call(new RemoteCallback<Path>() {
										@Override
										public void callback(final Path projPath) {
											//populate Project object from path
											projectPath = projPath;
											consoleLog("found Project Path: "+projPath);
											projectServices.call(new RemoteCallback<Project>() {
												@Override
												public void callback(final Project project) {
													activeProject = project;
													if(project!=null && repository!=null) {
														consoleLog("found Project Name : " + project.getProjectName());
														contextChangedEvent.fire(new ProjectContextChangeEvent( activeOrganizationalUnit, repository, project));
													}else{
														consoleLog("WARNING: DID NOT FIND Project !!!!!!!! ");
														
													}
												}
											}).resolveProject(projPath);
											
										}
									}).get(projectPathString);
									
						}
					}
 
				}).getOrganizationalUnits();
			}
		}).getRepository(repoPath);

	}

	@Perspective
	public PerspectiveDefinition getPerspective() {
        //When the Perspective definition is requested (i.e. the workbench is switching to *this* perspective) lookup the Project 
		perspective = buildPerspective();
		
		return this.perspective;

	}
	
	private PerspectiveDefinition buildPerspective() {

		this.perspective = new PerspectiveDefinitionImpl(
				PanelType.ROOT_LIST);
		perspective.setName(constants.Project_Authoring());

		final PanelDefinition west = new PanelDefinitionImpl(PanelType.SIMPLE);
		west.setWidth(400);
		west.addPart(new PartDefinitionImpl(new DefaultPlaceRequest(
				"org.kie.guvnor.explorer")));

		perspective.getRoot().insertChild(Position.WEST, west);

		getProjectPath();

		return perspective;
	}

	private void buildMenuBar() {
		this.menus = MenuFactory
				.newTopLevelMenu(constants.explore())
				.menus()
				.menu(constants.inboxIncomingChanges())
				.respondsWith(new Command() {
					@Override
					public void execute() {
						placeManager.goTo("Inbox");
					}
				})
				.endMenu()
				.menu(constants.inboxRecentlyEdited())
				.respondsWith(new Command() {
					@Override
					public void execute() {
						PlaceRequest p = new DefaultPlaceRequest("Inbox");
						p.addParameter("inboxname",
								InboxPresenter.RECENT_EDITED_ID);
						placeManager.goTo(p);
					}
				})
				.endMenu()
				.menu(constants.inboxRecentlyOpened())
				.respondsWith(new Command() {
					@Override
					public void execute() {
						PlaceRequest p = new DefaultPlaceRequest("Inbox");
						p.addParameter("inboxname",
								InboxPresenter.RECENT_VIEWED_ID);
						placeManager.goTo(p);
					}
				}).endMenu().endMenus().endMenu()
				.newTopLevelMenu(constants.newItem())
				.withItems(newResourcesMenu.getMenuItems()).endMenu()
				.newTopLevelMenu(constants.tools())
				.withItems(projectMenu.getMenuItems()).endMenu()
				.newTopLevelMenu(AppConstants.INSTANCE.Repository())
				.withItems(repositoryMenu.getMenuItems()).endMenu().build();
	}

	private void buildToolBar() {
		this.toolBar = new DefaultToolBar("guvnor.new.item");
		final String tooltip = AppConstants.INSTANCE.newItem();
		final Command command = new Command() {
			@Override
			public void execute() {
				newResourcePresenter.show();
			}
		};
		toolBar.addItem(new DefaultToolBarItem(IconType.FILE, tooltip, command));

	}

}
