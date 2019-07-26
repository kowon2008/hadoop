/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs.azurebfs.contracts.services;

import org.codehaus.jackson.annotate.JsonProperty;

import org.apache.hadoop.classification.InterfaceStability;

/**
 * The ListXPoliciesEntrySchema model.
 */
@InterfaceStability.Evolving
public class ListXPoliciesEntrySchema {
    /**
     * The name property.
     */
    @JsonProperty(value = "Name")
    private String name;

    /**
     * The accessType property.
     */
    @JsonProperty(value = "accessType")
    private String accessType;

    /**
     * The condition property.
     */
    @JsonProperty(value = "condition")
    private String condition;

    /**
     * The resource property.
     */
    @JsonProperty(value = "resource")
    private String resource;

    /**
     * The user property.
     */
    @JsonProperty(value = "user")
    private String user;

    /**
     * Get the name value.
     *
     * @return the name value
     */
    public String name() {
        return name;
    }

    /**
     * Set the name value.
     *
     * @param name the name value to set
     * @return the ListXPoliciesEntrySchema object itself.
     */
    public ListXPoliciesEntrySchema withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the accessType value.
     *
     * @return the accessType value
     */
    public String accessType() {
        return accessType;
    }

    /**
     * Set the accessType value.
     *
     * @param accessType the accessType value to set
     * @return the ListEntrySchema object itself.
     */
    public ListXPoliciesEntrySchema withAccessType(final String accessType) {
        this.accessType = accessType;
        return this;
    }

    /**
     * Get the condition value.
     *
     * @return the condition value
     */
    public String condition() {
        return condition;
    }

    /**
     * Set the condition value.
     *
     * @param condition the condition value to set
     * @return the ListXPoliciesEntrySchema object itself.
     */
    public ListXPoliciesEntrySchema withCondition(String condition) {
        this.condition = condition;
        return this;
    }

    /**
     * Get the resource value.
     *
     * @return the resource value
     */
    public String resource() {
        return resource;
    }

    /**
     * Set the resource value.
     *
     * @param resource the resource value to set
     * @return the ListXPoliciesEntrySchema object itself.
     */
    public ListXPoliciesEntrySchema withRResource(final String resource) {
        this.resource = resource;
        return this;
    }

    /**
     * Get the user value.
     *
     * @return the user value
     */
    public String user() {
        return user;
    }

    /**
     * Set the user value.
     *
     * @param user the user value to set
     * @return the ListXPoliciesEntrySchema object itself.
     */
    public ListXPoliciesEntrySchema withUser(final String user) {
        this.user = user;
        return this;
    }

}