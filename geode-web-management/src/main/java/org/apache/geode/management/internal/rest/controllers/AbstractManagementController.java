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

import org.apache.geode.internal.logging.LogService;
import org.apache.geode.internal.security.SecurityService;
import org.apache.geode.management.internal.JettyHelper;
import org.apache.geode.management.internal.api.LocatorClusterManagementService;
import org.apache.geode.management.internal.rest.responses.ManagementResponse;
import org.apache.geode.security.AuthenticationFailedException;
import org.apache.geode.security.NotAuthorizedException;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.context.ServletContextAware;

import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.servlet.ServletContext;

public class AbstractManagementController implements ServletContextAware {

  protected static final String MANAGEMENT_API_VERSION = "/v2";
  protected SecurityService securityService;
  protected LocatorClusterManagementService clusterManagementService;

  @Override
  public void setServletContext(ServletContext servletContext) {
    securityService = (SecurityService) servletContext
        .getAttribute(JettyHelper.SECURITY_SERVICE_SERVLET_CONTEXT_PARAM);
    clusterManagementService = (LocatorClusterManagementService) servletContext
        .getAttribute(JettyHelper.CLUSTER_MANAGEMENT_SERVICE_CONTEXT_PARAM);
  }

  private static final Logger logger = LogService.getLogger();

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ManagementResponse> internalError(final Exception e) {
    logger.error(e.getMessage(), e);
    return new ResponseEntity<>(new ManagementResponse(null, e.getMessage()),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(AuthenticationFailedException.class)
  public ResponseEntity<ManagementResponse> unauthorized(AuthenticationFailedException e) {
    return new ResponseEntity<>(new ManagementResponse(null, e.getMessage()),
        HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler({NotAuthorizedException.class, SecurityException.class})
  public ResponseEntity<ManagementResponse> forbidden(Exception e) {
    logger.info(e.getMessage());
    return new ResponseEntity<>(new ManagementResponse(null, e.getMessage()),
        HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(MalformedObjectNameException.class)
  public ResponseEntity<ManagementResponse> badRequest(final MalformedObjectNameException e) {
    logger.info(e.getMessage(), e);
    return new ResponseEntity<>(new ManagementResponse(null, e.getMessage()),
        HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(InstanceNotFoundException.class)
  public ResponseEntity<ManagementResponse> notFound(final InstanceNotFoundException e) {
    logger.info(e.getMessage(), e);
    return new ResponseEntity<>(new ManagementResponse(null, e.getMessage()),
        HttpStatus.NOT_FOUND);
  }

  /**
   * Handles an AccessDenied Exception thrown by a REST API web service endpoint, HTTP request
   * handler method.
   * <p/>
   *
   * @param cause the Exception causing the error.
   * @return a ResponseEntity with an appropriate HTTP status code (403 - Forbidden)
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ManagementResponse> handleException(
      final AccessDeniedException cause) {
    logger.info(cause.getMessage(), cause);
    return new ResponseEntity<>(new ManagementResponse(null, cause.getMessage()),
        HttpStatus.FORBIDDEN);
  }

  /**
   * Initializes data bindings for various HTTP request handler method parameter Java class types.
   *
   * @param dataBinder the DataBinder implementation used for Web transactions.
   * @see WebDataBinder
   * @see InitBinder
   */
  @InitBinder
  public void initBinder(final WebDataBinder dataBinder) {
    dataBinder.registerCustomEditor(String[].class,
        new StringArrayPropertyEditor(StringArrayPropertyEditor.DEFAULT_SEPARATOR, false));
  }
}
