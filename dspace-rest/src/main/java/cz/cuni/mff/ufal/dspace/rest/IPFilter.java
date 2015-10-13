package cz.cuni.mff.ufal.dspace.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Created by okosarko on 13.10.15.
 */
@Provider
public class IPFilter implements ContainerRequestFilter {

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String host = request.getRemoteHost();
        if(host.contains("mff.cuni.cz")){
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).
                    entity("Restricted access to this resource.").build());
        }

    }
}
