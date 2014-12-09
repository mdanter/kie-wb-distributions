package org.kie.workbench.drools.backend;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;

import org.guvnor.common.services.project.context.ProjectContextChangeEvent;
import org.guvnor.common.services.project.model.Project;
import org.guvnor.common.services.project.service.ProjectService;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.workbench.drools.shared.BuildServiceResult;
import org.kie.workbench.drools.shared.CustomBuildService;
import org.uberfire.backend.organizationalunit.OrganizationalUnit;
import org.uberfire.backend.organizationalunit.OrganizationalUnitService;
import org.uberfire.backend.repositories.Repository;
import org.uberfire.backend.repositories.RepositoryService;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.io.IOService;

@Service
@ApplicationScoped
public class CustomBuildServiceImpl implements CustomBuildService {
	
    @Inject
    private ProjectService projectService;

    @Inject
    @Named("ioStrategy")
    private IOService ioService;

    @Inject
    private RepositoryService repositoryService;

    @Inject
    private OrganizationalUnitService organizationalUnitService;

    @Inject
    private Event<ProjectContextChangeEvent> contextChangedEvent;

    @Override
    public BuildServiceResult build( final String _path ) {
    	
    	System.out.println("here 1");
    	
        if ( _path == null || _path.trim().isEmpty() ) {
        	System.out.println("here 2");
            return null;
        }
        System.out.println("here 3");
        final Path path = Paths.convert( ioService.get( URI.create( _path.trim() ) ) );
        System.out.println("here 4");
        final Project project = projectService.resolveProject( path );
        System.out.println("here 5");
        final Repository repo = repositoryService.getRepository( Paths.convert( Paths.convert( path ).getRoot() ) );
        System.out.println("here 6");
        OrganizationalUnit ou = null;
        System.out.println("here 7");
        for ( final OrganizationalUnit organizationalUnit : organizationalUnitService.getOrganizationalUnits() ) {
            if ( organizationalUnit.getRepositories().contains( repo ) ) {
                ou = organizationalUnit;
                System.out.println("Found OU!");
                break;
            }
        }

        if ( ou != null && repo != null && project != null ) {
        	System.out.println("about to fire context change event");
            contextChangedEvent.fire( new ProjectContextChangeEvent( ou, repo, project ) );
            System.out.println("fired context change event"+ou+repo+project);
            return new BuildServiceResult( ou, repo, project );
        }
        
        return null;
    }
}
