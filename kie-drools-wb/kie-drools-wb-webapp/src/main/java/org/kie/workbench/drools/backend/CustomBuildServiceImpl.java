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
        if ( _path == null || _path.trim().isEmpty() ) {
            return null;
        }

        final Path path = Paths.convert( ioService.get( URI.create( _path.trim() ) ) );
        final Project project = projectService.resolveProject( path );

        final Repository repo = repositoryService.getRepository( Paths.convert( Paths.convert( path ).getRoot() ) );
        OrganizationalUnit ou = null;
        for ( final OrganizationalUnit organizationalUnit : organizationalUnitService.getOrganizationalUnits() ) {
            if ( organizationalUnit.getRepositories().contains( repo ) ) {
                ou = organizationalUnit;
                break;
            }
        }

        if ( ou != null && repo != null && project != null ) {
            contextChangedEvent.fire( new ProjectContextChangeEvent( ou, repo, project ) );
            return new BuildServiceResult( ou, repo, project );
        }
        return null;
    }
}
