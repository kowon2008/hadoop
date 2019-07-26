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

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

import org.apache.hadoop.classification.InterfaceStability;

/**
 * The ListXPoliciesSchema model.
 */
@InterfaceStability.Evolving
public class ListXPoliciesSchema {
    /**
     * The policies property.
     */
    @JsonProperty(value = "policies")
    private List<ListXPoliciesEntrySchema> policies;

    /**
     * * Get the policies value.
     *
     * @return the policies value
     */
    public List<ListXPoliciesEntrySchema> policies() {
        return this.policies;
    }

    /**
     * Set the policies value.
     *
     * @param paths the policies value to set
     * @return the ListXPoliciesSchema object itself.
     */
    public ListXPoliciesSchema withXPolicies(final List<ListXPoliciesEntrySchema> policies) {
        this.policies = policies;
        return this;
    }

}
