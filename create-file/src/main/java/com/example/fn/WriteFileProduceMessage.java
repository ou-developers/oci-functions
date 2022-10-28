/*
** WriteFileProduceMessage version 1.2.
**
** Copyright (c) 2022 Oracle, Inc.
** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
*/

package com.example.fn;

import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.streaming.StreamAdminClient;
import com.oracle.bmc.streaming.StreamClient;
import com.oracle.bmc.streaming.model.PutMessagesDetails;
import com.oracle.bmc.streaming.model.PutMessagesDetailsEntry;
import com.oracle.bmc.streaming.model.PutMessagesResult;
import com.oracle.bmc.streaming.model.PutMessagesResultEntry;
import com.oracle.bmc.streaming.model.StreamSummary;
import com.oracle.bmc.streaming.requests.ListStreamsRequest;
import com.oracle.bmc.streaming.requests.PutMessagesRequest;
import com.oracle.bmc.streaming.responses.ListStreamsResponse;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class WriteFileProduceMessage {

    private ObjectStorage objStoreClient = null;

    final ResourcePrincipalAuthenticationDetailsProvider provider
            = ResourcePrincipalAuthenticationDetailsProvider.builder().build();

    public WriteFileProduceMessage() {
        try {
            //print env vars in Functions container
            System.err.println("OCI_RESOURCE_PRINCIPAL_VERSION " + System.getenv("OCI_RESOURCE_PRINCIPAL_VERSION"));
            System.err.println("OCI_RESOURCE_PRINCIPAL_REGION " + System.getenv("OCI_RESOURCE_PRINCIPAL_REGION"));
            System.err.println("OCI_RESOURCE_PRINCIPAL_RPST " + System.getenv("OCI_RESOURCE_PRINCIPAL_RPST"));
            System.err.println("OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM " + System.getenv("OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM"));

            System.err.println("NAMESPACE: " + System.getenv().get("NAMESPACE"));
            System.err.println("STREAM_ENDPOINT: " + System.getenv().get("STREAM_ENDPOINT"));
            System.err.println("STREAM_OCID: " + System.getenv().get("STREAM_OCID"));

            objStoreClient = new ObjectStorageClient(provider);

        } catch (Throwable ex) {
            System.err.println("Failed to instantiate ObjectStorage client - " + ex.getMessage());
        }

    } // end WriteFileProduceMessage constructor method

    public static class ObjectInfo {

        private String name;
        private String bucketName;
        private String content;
        public String getBucketName() {
            return bucketName;
        }
        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }
        public ObjectInfo() {
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getContent() {
            return content;
        }
        public void setContent(String content) {
            this.content = content;
        }
    } // End ObjectInfo static class

    private static void publishMessage(ObjectInfo objectInfo, StreamClient streamClient, String streamId) {

        String result = null;

        PutMessagesDetails putMessagesDetails = PutMessagesDetails.builder()
                .messages(Arrays.asList(PutMessagesDetailsEntry.builder()
                .key(objectInfo.name.getBytes(StandardCharsets.UTF_8))
                .value(objectInfo.content.getBytes(StandardCharsets.UTF_8)).build()))
                .build();

        PutMessagesRequest putMessagesRequest = PutMessagesRequest.builder()
                .putMessagesDetails(putMessagesDetails)
                .streamId(streamId)
                .build();

        PutMessagesResult putMessagesResult = streamClient.putMessages(putMessagesRequest).getPutMessagesResult();
        System.err.println("pushed message");

        for (PutMessagesResultEntry entry : putMessagesResult.getEntries()) {
            if (entry.getError() != null) {
                result = "Put message error " + entry.getErrorMessage();
                System.err.println(result);
            } else {
        result = "Message pushed to offset " + entry.getOffset() + " in partition " + entry.getPartition();
        System.err.println(result);
            }
        }

    } // End publishMessage method

    public String handle(ObjectInfo objectInfo) {

        String result = "FAILED";
        String objResult = "Obj Failed";
        String streamResult = "Stream Failed";

        if (objStoreClient == null) {
            System.err.println("There was a problem creating the ObjectStorage Client object. Please check logs");
            return result;
        }
        try {

            String nameSpace = System.getenv().get("NAMESPACE");

            // Put a new file in the indicated Object Storage Bucket
            PutObjectRequest por = PutObjectRequest.builder()
                    .namespaceName(nameSpace)
                    .bucketName(objectInfo.bucketName)
                    .objectName(objectInfo.name)
                    .putObjectBody(new ByteArrayInputStream(objectInfo.content.getBytes(StandardCharsets.UTF_8)))
                    .build();

            PutObjectResponse poResp = objStoreClient.putObject(por);
            objResult = "Successfully put to Object Storage with Filename=" + objectInfo.name + " Bucket=" + objectInfo.bucketName;
            System.err.println(objResult);

            // Import Stream OCID & Endpoint
            String ociMessageEndpoint = System.getenv().get("STREAM_ENDPOINT");
            String ociStreamOcid = System.getenv().get("STREAM_OCID");

            // Create a stream client using the provided message endpoint.
            StreamClient streamClient = StreamClient.builder().endpoint(ociMessageEndpoint).build(provider);

            // publish data from objectInfo as a message to the stream
            publishMessage(objectInfo, streamClient, ociStreamOcid);

            // if an exception is NOT thrown, log result...
            streamResult = "Successfully produced message to Stream with Key=" + objectInfo.name + " Message=" + objectInfo.content;
            System.err.println(streamResult);

        } catch (Throwable e) {
            // ...otherwise, create an error message
            System.err.println("Error storing object in bucket or writing to stream " + e.getMessage());
            result = "Error storing object in bucket or writing to stream " + e.getMessage();
        }

        return streamResult + "\n" + objResult;

    } // End Fn Function handle method

} // End WriteFileProduceMessage class
