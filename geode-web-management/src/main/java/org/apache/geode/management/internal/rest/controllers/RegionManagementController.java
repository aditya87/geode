/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.geode.management.internal.rest.controllers;

import org.apache.geode.cache.configuration.RegionConfig;
import org.apache.geode.management.internal.api.ClusterManagementResult;
import org.apache.geode.management.internal.exceptions.NoMembersException;
import org.apache.geode.management.internal.rest.responses.ManagementResponse;
import org.apache.geode.management.internal.rest.responses.Metadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.apache.geode.management.internal.rest.controllers.AbstractManagementController.MANAGEMENT_API_VERSION;

@Controller("regionManagement")
@RequestMapping(MANAGEMENT_API_VERSION)
public class RegionManagementController extends AbstractManagementController {

  @PreAuthorize("@securityService.authorize('DATA', 'MANAGE')")
  @RequestMapping(method = RequestMethod.POST, value = "/regions")
  public ResponseEntity<ManagementResponse> createRegion(
      @RequestBody RegionConfig regionConfig) {
    try {
      ClusterManagementResult result = clusterManagementService.create(regionConfig);
      ManagementResponse response = result.isSuccessful() ?
              new ManagementResponse(new Metadata("/geode/v2/regions/" + regionConfig.getName()), null) :
              new ManagementResponse(null, result.getCombinedErrorMessage());
      return new ResponseEntity<>(response, result.isSuccessful() ?
              HttpStatus.CREATED :
              HttpStatus.INTERNAL_SERVER_ERROR
      );
    } catch(NoMembersException e) {
      return new ResponseEntity<>(new ManagementResponse(null,
              "no members found to create cache element"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}