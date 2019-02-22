/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.testutils.resources;

import com.amazonaws.AmazonClientException;
import com.amazonaws.testutils.resources.RequiredResources.ResourceCreationPolicy;
import com.amazonaws.testutils.resources.TestResource.ResourceStatus;

public class TestResourceUtils {

    public static void createResource(TestResource resource, ResourceCreationPolicy policy)
            throws InterruptedException {
        ResourceStatus finalizedStatus = waitForFinalizedStatus(resource);
        if (policy == ResourceCreationPolicy.ALWAYS_RECREATE) {
            if (finalizedStatus != ResourceStatus.NOT_EXIST) {
                resource.delete(true);
            }
            resource.create(true);
        } else if (policy == ResourceCreationPolicy.REUSE_EXISTING) {
            switch (finalizedStatus) {
                case AVAILABLE:
                    System.out.println("Found existing resource " + resource
                            + " that could be reused...");
                    return;
                case EXIST_INCOMPATIBLE_RESOURCE:
                    resource.delete(true);
                    resource.create(true);
                case NOT_EXIST:
                    resource.create(true);
            }
        }
    }

    public static void deleteResource(TestResource resource) throws InterruptedException {
        ResourceStatus finalizedStatus = waitForFinalizedStatus(resource);
        if (finalizedStatus != ResourceStatus.NOT_EXIST) {
            resource.delete(false);
        }
    }

    public static ResourceStatus waitForFinalizedStatus(TestResource resource)
            throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        while (System.currentTimeMillis() < endTime) {
            ResourceStatus status = resource.getResourceStatus();
            if (status != ResourceStatus.TRANSIENT) {
                return status;
            }
            System.out.println("Waiting for " + resource + " to escape the transient state...");
            Thread.sleep(1000 * 10);
        }

        throw new AmazonClientException("Resource never escaped the transient state.");
    }
}
