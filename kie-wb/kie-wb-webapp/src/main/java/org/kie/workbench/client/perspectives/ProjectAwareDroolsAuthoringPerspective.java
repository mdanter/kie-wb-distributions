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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.guvnor.common.services.project.context.ProjectContextChangeEvent;
import org.guvnor.common.services.project.model.Project;
import org.guvnor.inbox.client.InboxPresenter;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.kie.workbench.client.resources.i18n.AppConstants;
import org.kie.workbench.common.screens.explorer.client.widgets.business.BusinessViewPresenterImpl;
import org.kie.workbench.common.screens.explorer.client.widgets.technical.TechnicalViewPresenterImpl;
import org.kie.workbench.common.screens.projecteditor.client.menu.ProjectMenu;
import org.kie.workbench.common.widgets.client.handlers.NewResourcePresenter;
import org.kie.workbench.common.widgets.client.handlers.NewResourcesMenu;
import org.kie.workbench.common.widgets.client.menu.RepositoryMenu;
import org.kie.workbench.shared.BuildServiceResult;
import org.kie.workbench.shared.CustomBuildService;
import org.uberfire.backend.repositories.Repository;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.annotations.Perspective;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPerspective;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.workbench.PanelManager;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.PanelDefinition;
import org.uberfire.workbench.model.PanelType;
import org.uberfire.workbench.model.PartDefinition;
import org.uberfire.workbench.model.PerspectiveDefinition;
import org.uberfire.workbench.model.Position;
import org.uberfire.workbench.model.impl.PanelDefinitionImpl;
import org.uberfire.workbench.model.impl.PartDefinitionImpl;
import org.uberfire.workbench.model.impl.PerspectiveDefinitionImpl;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.MenuItem;
import org.uberfire.workbench.model.menu.Menus;

import com.google.gwt.user.client.Window;

@ApplicationScoped
@WorkbenchPerspective(identifier = "projectAwareDroolsAuthoringPerspective")
public class ProjectAwareDroolsAuthoringPerspective {

    private AppConstants constants = AppConstants.INSTANCE;

    private String projectPathString;

    @Inject
    private PlaceManager placeManager;

    @Inject
    private NewResourcePresenter newResourcePresenter;

    @Inject
    private NewResourcesMenu newResourcesMenu;

    @Inject
    private ProjectMenu projectMenu;

    @Inject
    private PanelManager panelManager;

    @Inject
    private RepositoryMenu repositoryMenu;

    @Inject
    private Caller<CustomBuildService> customBuildService;

    private Path repoPath;
    private Path projectPath;
    private Project activeProject;
    private Repository activeRepository;

    @Inject
    private BusinessViewPresenterImpl businessViewPresenter;

    @Inject
    private TechnicalViewPresenterImpl technicalViewPresenter;

    native void consoleLog( String message ) /*-{
        console.log("projectAwareDroolsAuthoringPerspective: " + message);
    }-*/;

    @WorkbenchMenu
    public Menus getMenus() {
    	
    	List<MenuItem> newResourcesSubmenu = newResourcesMenu.getMenuItems();
    	
    	for(MenuItem item: newResourcesSubmenu){
    		
    		if(item.getCaption().equalsIgnoreCase("Project")){
    			newResourcesSubmenu.remove(item);
    		}
    	}
    	
        return MenuFactory
                .newTopLevelMenu( constants.explore() )
                .menus()
                .menu( constants.inboxIncomingChanges() )
                .respondsWith( new Command() {
                    @Override
                    public void execute() {
                        placeManager.goTo( "Inbox" );
                    }
                } )
                .endMenu()
                .menu( constants.inboxRecentlyEdited() )
                .respondsWith( new Command() {
                    @Override
                    public void execute() {
                        PlaceRequest p = new DefaultPlaceRequest( "Inbox" );
                        p.addParameter( "inboxname",
                                        InboxPresenter.RECENT_EDITED_ID );
                        placeManager.goTo( p );
                    }
                } )
                .endMenu()
                .menu( constants.inboxRecentlyOpened() )
                .respondsWith( new Command() {
                    @Override
                    public void execute() {
                        PlaceRequest p = new DefaultPlaceRequest( "Inbox" );
                        p.addParameter( "inboxname",
                                        InboxPresenter.RECENT_VIEWED_ID );
                        placeManager.goTo( p );
                    }
                } ).endMenu().endMenus().endMenu()
                .newTopLevelMenu( constants.newItem() )
                .withItems( newResourcesSubmenu ).endMenu()
                .newTopLevelMenu( constants.tools() )
                .withItems( projectMenu.getMenuItems() ).endMenu()
                .newTopLevelMenu( AppConstants.INSTANCE.Repository() )
                .withItems( repositoryMenu.getMenuItems() ).endMenu().build();
    }

    @OnStartup
    public void onStartup() {
        //gets the path param from a GET parameter and creates a Path object from it
        projectPathString = ( ( Window.Location.getParameterMap().containsKey( "path" ) ) ? Window.Location.getParameterMap().get( "path" ).get( 0 ) : "" );

//        projectPathString = "git://master@uf-playground/mortgages/";

        consoleLog( "STRING projectPath via GET: " + projectPathString );

        if ( projectPathString != null && !projectPathString.isEmpty() ) {
            customBuildService.call( new RemoteCallback<BuildServiceResult>() {
                @Override
                public void callback( final BuildServiceResult response ) {
                    if ( response != null ) {
                        businessViewPresenter.initialiseViewForActiveContext( response.getOrganizationalUnit(), response.getRepository(), response.getProject() );
                        technicalViewPresenter.initialiseViewForActiveContext( response.getOrganizationalUnit(), response.getRepository(), response.getProject() );
                    }
                }
            } ).build( projectPathString );
        }
    }

    @Perspective
    public PerspectiveDefinition getPerspective() {
        //When the Perspective definition is requested (i.e. the workbench is switching to *this* perspective) lookup the Project 
        final PerspectiveDefinitionImpl perspective = new PerspectiveDefinitionImpl(
                PanelType.ROOT_LIST );
        perspective.setName( constants.Project_Authoring() );

        final PanelDefinition west = new PanelDefinitionImpl( PanelType.SIMPLE );
        west.setWidth( 400 );
        west.addPart( new PartDefinitionImpl( new DefaultPlaceRequest(
                "org.kie.guvnor.explorer" ) ) );

        perspective.getRoot().insertChild( Position.WEST, west );

        return perspective;
    }

    private final List<PlaceRequest> placesToClose = new ArrayList<PlaceRequest>();

    public void onContextChange( @Observes ProjectContextChangeEvent event ) {
        placesToClose.clear();
        process( panelManager.getRoot().getParts() );
        process( panelManager.getRoot().getChildren() );

        for ( final PlaceRequest placeRequest : placesToClose ) {
            placeManager.forceClosePlace( placeRequest );
        }
    }

    private void process( final List<PanelDefinition> children ) {
        for ( final PanelDefinition child : children ) {
            process( child.getParts() );
            process( child.getChildren() );
        }
    }

    private void process( Collection<PartDefinition> parts ) {
        for ( final PartDefinition partDefinition : parts ) {
            if ( !partDefinition.getPlace().getIdentifier().equals( "org.kie.guvnor.explorer" ) ) {
                placesToClose.add( partDefinition.getPlace() );
            }
        }
    }

}
