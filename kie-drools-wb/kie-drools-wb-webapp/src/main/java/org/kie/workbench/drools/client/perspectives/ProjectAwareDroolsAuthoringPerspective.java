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
package org.kie.workbench.drools.client.perspectives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.guvnor.common.services.project.context.ProjectContextChangeEvent;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.kie.workbench.common.screens.explorer.client.widgets.business.BusinessViewPresenterImpl;
import org.kie.workbench.common.screens.explorer.client.widgets.business.BusinessViewWidget;
import org.kie.workbench.common.screens.explorer.client.widgets.navigator.Explorer;
import org.kie.workbench.common.screens.explorer.client.widgets.technical.TechnicalViewPresenterImpl;
import org.kie.workbench.common.screens.explorer.client.widgets.technical.TechnicalViewWidget;
import org.kie.workbench.common.screens.projecteditor.client.menu.ProjectMenu;
import org.kie.workbench.common.widgets.client.handlers.NewResourcePresenter;
import org.kie.workbench.common.widgets.client.handlers.NewResourcesMenu;
import org.kie.workbench.common.widgets.client.menu.RepositoryMenu;
import org.kie.workbench.drools.client.resources.i18n.AppConstants;
import org.kie.workbench.drools.shared.BuildServiceResult;
import org.kie.workbench.drools.shared.CustomBuildService;
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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;

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

    @Inject
    private BusinessViewPresenterImpl businessViewPresenter;

    @Inject
    private TechnicalViewPresenterImpl technicalViewPresenter;

    private final Command updateExplorer = new Command() {
        @Override
        public void execute() {
            Scheduler.get().scheduleDeferred( new com.google.gwt.user.client.Command() {
                @Override
                public void execute() {
                	consoleLog("In execute!");
                    //view( businessViewPresenter ).getExplorer().setVisible( false );
                    executeOnExpandNavigator( view( businessViewPresenter ).getExplorer() );
                    container( view( businessViewPresenter ).getExplorer() ).getElement().getElementsByTagName( "i" ).getItem( 0 ).getStyle().setDisplay( Style.Display.NONE );
                    container( view( businessViewPresenter ).getExplorer() ).getElement().getElementsByTagName( "ul" ).getItem( 0 ).getStyle().setDisplay( Style.Display.NONE );
                    container( view( technicalViewPresenter ).getExplorer() ).getElement().getElementsByTagName( "i" ).getItem( 0 ).getStyle().setDisplay( Style.Display.NONE );
                    container( view( technicalViewPresenter ).getExplorer() ).getElement().getElementsByTagName( "ul" ).getItem( 0 ).getStyle().setDisplay( Style.Display.NONE );
                }
            } );
        }
    };

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

        return MenuFactory.newTopLevelMenu( constants.newItem() )
                .withItems( newResourcesSubmenu ).endMenu()
                .build();
    }
    
    @OnStartup
    public void onStartup() {
        //gets the path param from a GET parameter and creates a Path object from it
        projectPathString = ( ( Window.Location.getParameterMap().containsKey( "path" ) ) ? Window.Location.getParameterMap().get( "path" ).get( 0 ) : "" );

//        projectPathString = "git://master@uf-playground/mortgages/";

        consoleLog( "**************** STRING projectPath via GET: " + projectPathString );

        if ( !projectPathString.isEmpty() ) {
        	consoleLog( ">>>>>>>>>>>>>>> projectPath NOT EMPTY: " + projectPathString );
            BuildServiceResult bsr = customBuildService.call( new RemoteCallback<BuildServiceResult>() {
                @Override
                public void callback( final BuildServiceResult response ) {
                    if ( response != null ) {
                    	
                    	consoleLog(response.getOrganizationalUnit().toString());
                    	consoleLog(response.getProject().toString());
                    	consoleLog(response.getRepository().toString());
                    	
                    	consoleLog("!!!!!!--------> I'm being called 1!");
                        updateExplorer.execute();
                        consoleLog("!!!!!!--------> I'm being called 2!");
                        businessViewPresenter.initialiseViewForActiveContext( response.getOrganizationalUnit(), response.getRepository(), response.getProject() );
                        consoleLog("!!!!!!--------> I'm being called 3!");
                        technicalViewPresenter.initialiseViewForActiveContext( response.getOrganizationalUnit(), response.getRepository(), response.getProject() );
                        consoleLog("!!!!!!--------> I'm being called 4!");
                    }
                }
            } ).build( projectPathString );
            
            consoleLog("Switching to " + bsr.getOrganizationalUnit() + " -- " + bsr.getRepository() + " -- " + bsr.getProject());
        }else{
        	consoleLog("==============> PATH WAS EMPTY!");
        }
        
        
    }

    @Perspective
    public PerspectiveDefinition getPerspective() {
        //When the Perspective definition is requested (i.e. the workbench is switching to *this* perspective) lookup the Project 
        final PerspectiveDefinitionImpl perspective = new PerspectiveDefinitionImpl(
                PanelType.ROOT_LIST );
        perspective.setName( constants.project_authoring() );

        final PanelDefinition west = new PanelDefinitionImpl( PanelType.SIMPLE );
        west.setWidth( 400 );
        west.addPart( new PartDefinitionImpl( new DefaultPlaceRequest(
                "org.kie.guvnor.explorer" ) ) );

        perspective.getRoot().insertChild( Position.WEST, west );

        return perspective;
    }

    private final List<PlaceRequest> placesToClose = new ArrayList<PlaceRequest>();

    public void onContextChange( @Observes ProjectContextChangeEvent event ) {
    	
    	consoleLog(event.getOrganizationalUnit().toString()+event.getPackage().toString()+event.getProject().toString()+event.getRepository().toString());
        updateExplorer.execute();
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

    native BusinessViewWidget view( final BusinessViewPresenterImpl from ) /*-{
        return from.@org.kie.workbench.common.screens.explorer.client.widgets.business.BusinessViewPresenterImpl::view;
    }-*/;

    native TechnicalViewWidget view( final TechnicalViewPresenterImpl from ) /*-{
        return from.@org.kie.workbench.common.screens.explorer.client.widgets.technical.TechnicalViewPresenterImpl::view;
    }-*/;

    native FlowPanel container( final Explorer from ) /*-{
        return from.@org.kie.workbench.common.screens.explorer.client.widgets.navigator.Explorer::container;
    }-*/;

    native void executeOnExpandNavigator( final Explorer from ) /*-{
        from.@org.kie.workbench.common.screens.explorer.client.widgets.navigator.Explorer::onExpandNavigator()();
    }-*/;

}
