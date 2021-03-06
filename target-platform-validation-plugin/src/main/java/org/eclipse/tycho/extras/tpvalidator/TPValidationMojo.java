/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mickael Istria (Red Hat). - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.tpvalidator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;
import org.eclipse.tycho.p2.resolver.TargetDefinitionFile;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;

/**
 * Validates that specified target platforms (.target files) contents can be resolved.
 * 
 * @goal validate-target-platform
 * @phase validate
 */
public class TPValidationMojo extends AbstractMojo {
    /**
     * .target files to validate
     * 
     * @parameter
     * @required
     */
    private File[] targetFiles;

    /**
     * whether to fail build or just print a warning when a validation fails
     * 
     * @parameter default-value="true"
     */
    private boolean failOnError;

    /** @parameter default-value="JavaSE-1.6" */
    private String executionEnvironment;

    /** @component */
    protected EquinoxServiceFactory equinox;

    /** @component */
    private Logger logger;

    private P2ResolverFactory factory;
    protected P2Resolver p2;

    public void execute() throws MojoExecutionException {
        this.factory = this.equinox.getService(P2ResolverFactory.class);
        this.p2 = this.factory.createResolver(new MavenLoggerAdapter(this.logger, false));

        List<TPError> errors = new ArrayList<TPError>();
        for (File targetFile : this.targetFiles) {
            try {
                validateTarget(targetFile);
                this.logger.info("OK!");
            } catch (TPError ex) {
                this.logger.info("Failed, see Error log below");
                errors.add(ex);
            }
        }

        if (!errors.isEmpty()) {
            String message = createErrorMessage(errors);
            this.logger.error(message);
            if (this.failOnError) {
                throw new MojoExecutionException(message);
            }
        }
    }

    private String createErrorMessage(List<TPError> errors) {
        StringBuilder res = new StringBuilder();
        res.append("Validation found errors in ");
        res.append(errors.size());
        res.append(" .target files:");
        res.append("\n");
        for (TPError error : errors) {
            res.append(error.getMessage(this.logger.isDebugEnabled()));
            res.append("\n");
        }
        return res.toString();
    }

    private void validateTarget(File targetFile) throws TPError {
        try {
            // create resolver
            this.logger.info("Validating " + targetFile + "...");
            TargetPlatformConfigurationStub tpConfiguration = new TargetPlatformConfigurationStub();

            TargetDefinitionFile targetDefinition = TargetDefinitionFile.read(targetFile);

            tpConfiguration.setEnvironments(Collections.singletonList(TargetEnvironment.getRunningEnvironment()));
            tpConfiguration.addTargetDefinition(targetDefinition);
            P2ResolutionResult result = this.p2.resolveMetadata(tpConfiguration, executionEnvironment);
        } catch (Exception ex) {
            throw new TPError(targetFile, ex);
        }
    }

}
