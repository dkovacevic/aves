package com.aves.server.clients;


import com.aves.server.model.User;
import com.aves.server.tools.Logger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class SwisscomClient {
    private ObjectMapper mapper = new ObjectMapper();
    private WebTarget sign;
    private WebTarget pending;

    public SwisscomClient(Client httpClient) {
        sign = httpClient
                .target("https://ais.swisscom.com/AIS-Server/rs/v1.0/sign");

        pending = httpClient
                .target("https://ais.swisscom.com/AIS-Server/rs/v1.0/pending");
    }

    public SignResponse sign(User signer, UUID documentId, String hash) throws IOException {
        RootSignRequest request = new RootSignRequest();
        InputDocuments inputDocuments = request.signRequest.inputDocuments;

        inputDocuments.documentHash.hash = hash;
        inputDocuments.documentHash.documentId = documentId;

        CertificateRequest certificateRequest = request.signRequest.optionalInputs.certificateRequest;
        certificateRequest.distinguishedName = getDistinguishableName(
                signer.firstname,
                signer.lastname,
                signer.email,
                signer.country);

        Phone phone = certificateRequest.stepUpAuthorisation.phone;
        phone.language = signer.locale;
        phone.phoneNumber = signer.phone.replace("+", "");
        phone.message = String.format("Please confirm the signing of the document: %s", documentId);
        //phone.serialNumber = "SAS01E0D9GAI7OO1";

        Logger.debug(mapper.writeValueAsString(request));

        Response res = sign
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(request, MediaType.APPLICATION_JSON));

        if (res.getStatus() != 200)
            throw new IOException(res.readEntity(String.class));

        String entity = res.readEntity(String.class);

        Logger.debug(entity);

        RootSignResponse response = mapper.readValue(entity, RootSignResponse.class);
        return response.signResponse;
    }

    public SignResponse pending(UUID responseId) throws IOException {
        RootPendingRequest request = new RootPendingRequest(responseId);

        Response res = pending
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(request, MediaType.APPLICATION_JSON));

        if (res.getStatus() != 200)
            throw new IOException(res.readEntity(String.class));

        String entity = res.readEntity(String.class);
        ObjectMapper mapper = new ObjectMapper();

        RootSignResponse response = mapper.readValue(entity, RootSignResponse.class);
        return response.signResponse;
    }

    private String getDistinguishableName(String first, String last, String email, String country) {
        return String.format("CN=TEST %s %s, givenname=%s, surname=%s, C=%s, emailAddress=%s",
                first, last, first, last, country, email);
    }

    ///////////////// Sign Request ///////////////////////////
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RootSignRequest {
        @JsonProperty("SignRequest")
        public SignRequest signRequest;

        public RootSignRequest() {
            signRequest = new SignRequest();
            signRequest.requestId = UUID.randomUUID();
        }
    }

    public static class SignRequest {
        @JsonProperty("@Profile")
        public String profile = "http://ais.swisscom.ch/1.1";
        @JsonProperty("@RequestID")
        public UUID requestId;
        @JsonProperty("InputDocuments")
        public InputDocuments inputDocuments = new InputDocuments();
        @JsonProperty("OptionalInputs")
        public OptionalInputs optionalInputs = new OptionalInputs();
    }

    public static class InputDocuments {
        @JsonProperty("DocumentHash")
        public DocumentHash documentHash = new DocumentHash();
    }

    public static class DocumentHash {
        @JsonProperty("@ID")
        public UUID documentId;
        @JsonProperty("dsig.DigestValue")
        public String hash;
        @JsonProperty("dsig.DigestMethod")
        public DigestMethod digestMethod = new DigestMethod();
    }

    public static class DigestMethod {
        @JsonProperty("@Algorithm")
        public String algorithm = "http://www.w3.org/2001/04/xmlenc#sha256";
    }

    public static class AddTimestamp {
        @JsonProperty("@Type")
        public String type = "urn:ietf:rfc:3161";
    }

    public static class ClaimedIdentity {
        @JsonProperty("Name")
        public String name = "ais-90days-trial-OTP:OnDemand-Advanced";
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OptionalInputs {
        @JsonProperty("sc.CertificateRequest")
        public CertificateRequest certificateRequest = new CertificateRequest();
        @JsonProperty("sc.SignatureStandard")
        public String signatureStandard = "PADES";
        @JsonProperty("sc.AddRevocationInformation")
        public AddRevocationInformation addRevocationInformation = new AddRevocationInformation();
        @JsonProperty("SignatureType")
        public String SignatureType = "urn:ietf:rfc:3369";
        @JsonProperty("AdditionalProfile")
        public String[] additionalProfile = new String[]{
                "http://ais.swisscom.ch/1.0/profiles/batchprocessing",
                "urn:oasis:names:tc:dss:1.0:profiles:timestamping",
                "http://ais.swisscom.ch/1.0/profiles/ondemandcertificate",
                "urn:oasis:names:tc:dss:1.0:profiles:asynchronousprocessing",
                "http://ais.swisscom.ch/1.1/profiles/redirect",
        };
        @JsonProperty("AddTimestamp")
        public AddTimestamp addTimestamp = new AddTimestamp();
        @JsonProperty("ClaimedIdentity")
        public ClaimedIdentity claimedIdentity = new ClaimedIdentity();
    }

    public static class AddRevocationInformation {
        @JsonProperty("@Type")
        public String type = "BOTH";
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CertificateRequest {
        @JsonProperty("sc.StepUpAuthorisation")
        public StepUpAuthorisation stepUpAuthorisation = new StepUpAuthorisation();
        @JsonProperty("sc.DistinguishedName")
        public String distinguishedName;
    }

    public static class StepUpAuthorisation {
        @JsonProperty("sc.Phone")
        public Phone phone = new Phone();
    }

    public static class Phone {
        @JsonProperty("sc.Language")
        public String language;
        @JsonProperty("sc.MSISDN")
        public String phoneNumber;
        @JsonProperty("sc.Message")
        public String message;
        @JsonProperty("sc.SerialNumber")
        public String serialNumber;
    }
    ///////////////// Sign Request ///////////////////////////


    ///////////////// Sign Response ///////////////////////////
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RootSignResponse {
        @JsonProperty("SignResponse")
        public SignResponse signResponse;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SignResponse {
        @JsonProperty("@Profile")
        public String profile = "http://ais.swisscom.ch/1.1";
        @JsonProperty("@RequestID")
        public UUID requestId;
        @JsonProperty("OptionalOutputs")
        public OptionalOutputs optionalOutputs;
        @JsonProperty("SignatureObject")
        public SignatureObject signature;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OptionalOutputs {
        @JsonProperty("async.ResponseID")
        public UUID responseId;
        @JsonProperty("sc.StepUpAuthorisationInfo")
        public StepUpAuthorisationInfo stepUpAuthorisationInfo;
        @JsonProperty("sc.RevocationInformation")
        public RevocationInformation revocationInformation;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RevocationInformation {
        @JsonProperty("sc.CRLs")
        public HashMap<String, String> CRLs;
        @JsonProperty("sc.OCSPs")
        public HashMap<String, String> OCSPs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StepUpAuthorisationInfo {
        @JsonProperty("sc.Result")
        public _Result result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class _Result {
        @JsonProperty("sc.ConsentURL")
        public String url;
        @JsonProperty("sc.SerialNumber")
        public String serialNumber;
        @JsonProperty("SignatureObject")
        public SignatureObject signatureObject;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SignatureObject {
        @JsonProperty("Other")
        public Other other;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Other {
        @JsonProperty("sc.SignatureObjects")
        public SignatureObjects signatureObjects;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SignatureObjects {
        @JsonProperty("sc.ExtendedSignatureObject")
        public ExtendedSignatureObject extendedSignatureObject;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExtendedSignatureObject {
        @JsonProperty("@WhichDocument")
        public UUID documentId;
        @JsonProperty("Base64Signature")
        public Base64Signature base64Signature;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Base64Signature {
        @JsonProperty("@Type")
        public String type;
        @JsonProperty("$")
        public String value;
    }
    ///////////////// Sign Response ///////////////////////////

    ///////////////// Pending Request ////////////////////////////
    public static class RootPendingRequest {
        @JsonProperty("async.PendingRequest")
        public PendingRequest pendingRequest = new PendingRequest();

        public RootPendingRequest(UUID responseId) {
            pendingRequest.optionalInputs.responseId = responseId;
        }
    }

    public static class PendingRequest {
        @JsonProperty("@Profile")
        public String profile = "http://ais.swisscom.ch/1.0";
        @JsonProperty("OptionalInputs")
        public PendingOptionalInputs optionalInputs = new PendingOptionalInputs();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PendingOptionalInputs {
        @JsonProperty("ClaimedIdentity")
        public ClaimedIdentity claimedIdentity = new ClaimedIdentity();
        @JsonProperty("async.ResponseID")
        public UUID responseId;
    }
    ///////////////// Pending Request ////////////////////////////

}

