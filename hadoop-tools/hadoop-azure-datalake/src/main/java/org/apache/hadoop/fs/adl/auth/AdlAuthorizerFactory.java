/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs.adl.auth;

import java.io.IOException;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.adl.AdlConfKeys;

/**
 * Factory class for providing an instance of {@link AdlAuthorizer}
 */
@InterfaceAudience.Private
@InterfaceStability.Evolving
public final class AdlAuthorizerFactory {

  private AdlAuthorizerFactory() {

  }

  /**
   * Factory method to handle the creation and initialization of the external authorization class.
   *
   * @param configuration The file system configuration
   * @return Instantiated authorizer. If no value is present for the configuration, this will return null.
   * @throws IOException if this class is not found on the classpath or it fails to be instantiated.
   */
  public static AdlAuthorizer create(Configuration configuration) throws IOException {
    String authClassName = configuration.get(AdlConfKeys.ADL_EXTERNAL_AUTHORIZATION_CLASS);
    AdlAuthorizer authorizer = null;

    try {
      if (authClassName != null) {
        @SuppressWarnings("unchecked")
        Class<AdlAuthorizer> authClass = (Class<AdlAuthorizer>) configuration.getClassByName(authClassName);
        AdlAuthorizer adlAuthorizer = authClass.newInstance();
        adlAuthorizer.init(configuration);
        authorizer = adlAuthorizer;
      }
    } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
      throw new IOException(e);
    }
    return authorizer;
  }
}
