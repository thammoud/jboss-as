/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.server.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * A processor which creates a service loader index.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceLoaderProcessor implements DeploymentUnitProcessor {

    private static final Logger log = Logger.getLogger("org.jboss.as.server.deployment.service-loader");

    private static final Pattern VALID_NAME = Pattern.compile("(?:[a-zA-Z0-9_]+\\.)*[a-zA-Z0-9_]+");

    /** {@inheritDoc} */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final Map<String, List<String>> foundServices = new HashMap<String, List<String>>();
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (deploymentRoot != null) {
            processRoot(deploymentRoot, foundServices);
        }
        final AttachmentList<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
        if (resourceRoots != null) {
            for (ResourceRoot resourceRoot : resourceRoots) {
                processRoot(resourceRoot, foundServices);
            }
        }
        deploymentUnit.putAttachment(Attachments.SERVICES, new ServicesAttachment(foundServices));
    }

    private void processRoot(final ResourceRoot resourceRoot, final Map<String, List<String>> foundServices) throws DeploymentUnitProcessingException {
        final VirtualFile virtualFile = resourceRoot.getRoot();
        final VirtualFile child = virtualFile.getChild("META-INF/services");
        for (VirtualFile serviceType : child.getChildren()) {
            final String name = serviceType.getName();
            if (VALID_NAME.matcher(name).matches()) try {
                List<String> list = foundServices.get(name);
                if (list == null) {
                    foundServices.put(name, list = new ArrayList<String>());
                }
                final InputStream stream = serviceType.openStream();
                try {
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final int commentIdx = line.indexOf('#');
                        final String className;
                        if (commentIdx == -1) {
                            className = line.trim();
                        } else {
                            className = line.substring(0, commentIdx).trim();
                        }
                        if (className.length() == 0) {
                            continue;
                        }
                        if (! VALID_NAME.matcher(className).matches()) {
                            log.warnf("Encountered invalid class name \"%s\" for service type \"%s\"", className, name);
                        }
                        list.add(className);
                    }
                } finally {
                    VFSUtils.safeClose(stream);
                }
            } catch (IOException e) {
                throw new DeploymentUnitProcessingException("Failed to read '" + child + "'", e);
            }
        }
    }

    /** {@inheritDoc} */
    public void undeploy(final DeploymentUnit context) {
        context.removeAttachment(Attachments.SERVICES);
    }
}
