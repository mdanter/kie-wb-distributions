package org.kie.workbench.shared;

import org.jboss.errai.bus.server.annotations.Remote;

@Remote
public interface CustomBuildService {

    BuildServiceResult build( final String path );
}
