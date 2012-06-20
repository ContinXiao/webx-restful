/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.alibaba.webx.restful.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MediaType;

import com.alibaba.webx.restful.uri.PathPattern;
import com.google.common.collect.Lists;

/**
 * Model of a method available on a resource. Covers resource method, sub-resource method and sub-resource locator.
 * 
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ResourceMethod implements Routed, Producing, Consuming {

    /**
     * Resource method classification based on the recognized JAX-RS resource method types.
     */
    public static enum JaxrsType {
        /**
         * JAX-RS resource method.
         * <p/>
         * Does not have a path template assigned. Is assigned to a particular HTTP method.
         */
        RESOURCE_METHOD {

            @Override
            PathPattern createPatternFor(String pathTemplate) {
                // template is ignored.
                return PathPattern.END_OF_PATH_PATTERN;
            }
        },
        /**
         * JAX-RS sub-resource method.
         * <p/>
         * Has a sub-path template assigned and is assigned to a particular HTTP method.
         */
        SUB_RESOURCE_METHOD {

            @Override
            PathPattern createPatternFor(String pathTemplate) {
                return new PathPattern(pathTemplate, PathPattern.RightHandPath.capturingZeroSegments);
            }
        },
        /**
         * JAX-RS sub-resource locator.
         * <p/>
         * Has a sub-path template assigned but is not assigned to any particular HTTP method. Instead it produces a
         * sub-resource instance that should be further used in the request URI matching.
         */
        SUB_RESOURCE_LOCATOR {

            @Override
            PathPattern createPatternFor(String pathTemplate) {
                return new PathPattern(pathTemplate, PathPattern.RightHandPath.capturingZeroOrMoreSegments);
            }
        };

        /**
         * Create a proper matching path pattern from the provided template for the selected method type.
         * 
         * @param pathTemplate method path template.
         * @return method matching path pattern.
         */
        abstract PathPattern createPatternFor(String pathTemplate);

        private static JaxrsType classify(String httpMethod, String methodPath) {
            if (httpMethod != null) {
                if (!httpMethod.isEmpty()) {
                    if (methodPath == null || methodPath.isEmpty() || "/".equals(methodPath)) {
                        return RESOURCE_METHOD;
                    } else {
                        return SUB_RESOURCE_METHOD;
                    }
                }
            } else if (!methodPath.isEmpty()) {
                return SUB_RESOURCE_LOCATOR;
            }

            // TODO L10N
            throw new IllegalStateException(
                                            String.format("Unknown resource method model type: HTTP method = '%s', method path = '%s'.",
                                                          httpMethod, methodPath));
        }
    }

    // JAX-RS method type
    private final JaxrsType       type;
    // HttpMethod
    private final String          httpMethod;
    // Routed
    private final String          path;
    private final PathPattern     pathPattern;
    // Consuming & Producing
    private final List<MediaType> consumedTypes;
    private final List<MediaType> producedTypes;

    // Invocable
    private final Invocable       invocable;

    public ResourceMethod(final String httpMethod, final String path, final Collection<MediaType> consumedTypes,
                          final Collection<MediaType> producedTypes, final Invocable invocable){

        this.type = JaxrsType.classify(httpMethod, path);

        this.httpMethod = (httpMethod == null) ? httpMethod : httpMethod.toUpperCase();

        this.path = path;
        this.pathPattern = type.createPatternFor(path);

        this.consumedTypes = Collections.unmodifiableList(Lists.newArrayList(consumedTypes));
        this.producedTypes = Collections.unmodifiableList(Lists.newArrayList(producedTypes));
        this.invocable = invocable;
    }

    /**
     * Get the JAX-RS method type.
     * 
     * @return the JAX-RS method type.
     */
    public JaxrsType getType() {
        return type;
    }

    /**
     * Get the associated HTTP method.
     * <p/>
     * May return {@code null} in case the method represents a sub-resource locator.
     * 
     * @return the associated HTTP method, or {@code null} in case this method represents a sub-resource locator.
     */
    public String getHttpMethod() {
        return httpMethod;
    }

    /**
     * Get the invocable method model.
     * 
     * @return invocable method model.
     */
    public Invocable getInvocable() {
        return invocable;
    }

    // Routed

    /**
     * {@inheritDoc}
     * <p/>
     * In case of a resource method, an empty string is returned.
     * 
     * @return the path directly assigned to the method or an empty string in case the method represents a resource
     * method.
     */
    @Override
    public String getPath() {
        return path;
    }

    @Override
    public PathPattern getPathPattern() {
        return pathPattern;
    }

    // Consuming
    @Override
    public List<MediaType> getConsumedTypes() {
        return consumedTypes;
    }

    // Producing
    @Override
    public List<MediaType> getProducedTypes() {
        return producedTypes;
    }

    @Override
    public String toString() {
        return "ResourceMethod{" + "httpMethod=" + httpMethod + ", path=" + path + ", consumedTypes=" + consumedTypes
               + ", producedTypes=" + producedTypes + ", invocable=" + invocable + '}';
    }
}
