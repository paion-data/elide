/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.utils;

import jakarta.ws.rs.core.UriInfo;

import java.net.URI;

/**
 * Utility class which is shared by Resources/Controllers.
 */
public class ResourceUtils {

    /**
     * Resolve value of base url from UriInfo.
     * @param uriInfo UriInfo
     * @return baseUrl
     */
     public static String resolveBaseUrl(UriInfo uriInfo) {
         URI uri = uriInfo.getBaseUri();
         StringBuilder str = new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
         if (uri.getPort() != -1) {
             str.append(":" + uri.getPort());
         }
         return str.toString();
     }
}
