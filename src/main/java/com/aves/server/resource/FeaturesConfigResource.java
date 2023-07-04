package com.aves.server.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api
@Path("/feature-configs")
@Produces(MediaType.APPLICATION_JSON)
public class FeaturesConfigResource {
    @GET
    @ApiOperation(value = "Get Feature Configs", response = _Config.class)
    public Response getConfig() {
        return Response.
                ok(new _Config()).
                build();
    }

    public static class _Config {
        public _Status appLock = new _Status();
        public _Status classifiedDomains = new _Status();
        public _Status conferenceCalling = new _Status();
        public _Status conversationGuestLinks = new _Status();
        public _Status digitalSignatures = new _Status();
        public _Status exposeInvitationURLsToTeamAdmin = new _Status();
        public _Status fileSharing = new _Status();
        public _Status legalhold = new _Status();
        public _Status mls = new _Status();
        public _Status mlsE2EId = new _Status();
        public _Status mlsMigration = new _Status();
        public _Status outlookCalIntegration = new _Status();
        public _Status searchVisibility = new _Status();
        public _Status searchVisibilityInbound = new _Status();
        public _Status selfDeletingMessages = new _Status();
        public _Status sndFactorPasswordChallenge = new _Status();
        public _Status sso = new _Status();
        public _Status validateSAMLemails = new _Status();

    }

    public static class _Status {
        public String status = "enabled";
        public String lockStatus = "unlocked";
    }
}
