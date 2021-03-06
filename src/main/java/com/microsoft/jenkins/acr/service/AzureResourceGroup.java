/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.acr.service;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.resources.ResourceGroup;

import java.util.ArrayList;
import java.util.Collection;

public final class AzureResourceGroup extends AzureService {

    private AzureResourceGroup() {
    }

    public static AzureResourceGroup getInstance() {
        return SingletonInstance.getInstance();
    }

    public Collection<String> listResourceGroupNames() {
        Collection<String> result = new ArrayList<String>();
        PagedList<ResourceGroup> groupList = this.getClient().resourceGroups().list();
        for (ResourceGroup group : groupList) {
            result.add(group.name());
        }
        return result;
    }

    private static final class SingletonInstance {
        private static final AzureResourceGroup INSTANCE = new AzureResourceGroup();

        private SingletonInstance() {

        }

        public static AzureResourceGroup getInstance() {
            return INSTANCE;
        }
    }
}
