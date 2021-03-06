/**
 * Copyright (C) 2015 John Casey (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.propulsor.deploy.resteasy;

import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.core.Application;

import org.commonjava.propulsor.deploy.resteasy.helper.CdiInjectorFactoryImpl;
import org.commonjava.propulsor.deploy.resteasy.helper.RequestScopeListener;
import org.commonjava.propulsor.deploy.undertow.UndertowDeploymentProvider;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Is it really right to make this extend Application?? Not sure...
public class ResteasyDeploymentProvider
    extends Application
    implements UndertowDeploymentProvider
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<RestResources> resources;

    @Inject
    private Instance<RestProvider> providers;

    @Inject
    private ResteasyAppConfig config;

    private Set<Class<?>> resourceClasses;

    private Set<Class<?>> providerClasses;

    protected ResteasyDeploymentProvider()
    {
    }

    public ResteasyDeploymentProvider( final Set<Class<?>> resourceClasses, final Set<Class<?>> providerClasses,
                                       final ResteasyAppConfig config )
    {
        this.resourceClasses = resourceClasses;
        this.providerClasses = providerClasses;
        this.config = config;
    }

    @PostConstruct
    public void cdiInit()
    {
        this.resourceClasses = new HashSet<>();
        for ( final RestResources restResources : resources )
        {
            this.resourceClasses.add( restResources.getClass() );
        }

        this.providerClasses = new HashSet<>();
        for ( final RestProvider restProvider : providers )
        {
            this.providerClasses.add( restProvider.getClass() );
        }
    }

    @Override
    public DeploymentInfo getDeploymentInfo()
    {
        final ResteasyDeployment deployment = new ResteasyDeployment();

        //        deployment.getActualResourceClasses()
        //                  .addAll( resourceClasses );
        //
        //        deployment.getActualProviderClasses()
        //                  .addAll( providerClasses );

        deployment.setApplication( this );
        deployment.setInjectorFactoryClass( CdiInjectorFactoryImpl.class.getName() );

        final ServletInfo resteasyServlet = Servlets.servlet( "REST", HttpServlet30Dispatcher.class )
                                                    .setAsyncSupported( true )
                                                    .setLoadOnStartup( 1 )
                                                    .addMappings( config.getJaxRsMappings() );

        return new DeploymentInfo().addListener( Servlets.listener( RequestScopeListener.class ) )
                                   .addServletContextAttribute( ResteasyDeployment.class.getName(), deployment )
                                   .addServlet( resteasyServlet )
                                   .setClassLoader( ClassLoader.getSystemClassLoader() );
    }

    @Override
    public Set<Class<?>> getClasses()
    {
        final Set<Class<?>> allClasses = new HashSet<>();
        allClasses.addAll( resourceClasses );

        // TODO: This might not be right...
        allClasses.addAll( providerClasses );

        return allClasses;
    }

}
