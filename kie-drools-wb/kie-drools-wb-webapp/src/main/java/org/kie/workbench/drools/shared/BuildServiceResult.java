package org.kie.workbench.drools.shared;

import org.guvnor.common.services.project.model.Project;
import org.jboss.errai.common.client.api.annotations.Portable;
import org.uberfire.backend.organizationalunit.OrganizationalUnit;
import org.uberfire.backend.repositories.Repository;

/**
 * TODO: update me
 */
@Portable
public class BuildServiceResult {

    private OrganizationalUnit organizationalUnit;
    private Repository repository;
    private Project project;

    public BuildServiceResult() {

    }

    public BuildServiceResult( final OrganizationalUnit organizationalUnit,
                               final Repository repository,
                               final Project project ) {
        this.organizationalUnit = organizationalUnit;
        this.repository = repository;
        this.project = project;
    }

    public OrganizationalUnit getOrganizationalUnit() {
        return organizationalUnit;
    }

    public Repository getRepository() {
        return repository;
    }

    public Project getProject() {
        return project;
    }
}
