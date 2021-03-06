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
package org.commonjava.propulsor.boot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public abstract class BootOptions {

    public static final String BOOT_DEFAULTS_PROP = "boot.properties";

    @Option(name = "-h", aliases = { "--help" }, usage = "Print this and exit")
    private boolean help;

    private StringSearchInterpolator interp;

    private Properties bootProps;

    private String homeDir;

    @Option(name = "-f", aliases = { "--config" }, usage = "Specify the configuration file")
    private String config;

    public abstract String getHomeSystemProperty();

    public abstract String getConfigSystemProperty();

    public abstract String getHomeEnvar();

    protected BootOptions()
    {
    }

    protected BootOptions( final String homeDir )
    {
        this.homeDir = homeDir;
    }

    protected void loadApplicationOptions() {
    }

    protected void setApplicationSystemProperties(final Properties properties) {
    }

    public void load(final File bootDefaults, final String home)
            throws IOException, InterpolationException {
        homeDir = home;
        bootProps = new Properties();

        if (bootDefaults != null && bootDefaults.exists()) {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(bootDefaults);

                bootProps.load(stream);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }

        loadApplicationOptions();
    }

    protected final void setSystemProperties() {
        final Properties properties = System.getProperties();

        properties.setProperty(getHomeSystemProperty(),  assertPropertyIsNotNull(getHomeDir(), "homeDir is not specified"));
        properties.setProperty( getConfigSystemProperty(), assertPropertyIsNotNull(getConfig(), "config is not specified"));
        setApplicationSystemProperties( properties );
        System.setProperties(properties);
    }

    private final String assertPropertyIsNotNull(String value, String errorMessage) {
        if (value == null)
            throw new IllegalStateException(errorMessage);
        return value;
    }

    protected final Properties getBootProperties() {
        return bootProps;
    }

    protected final String resolve(final String value)
            throws InterpolationException {
        if (value == null || value.trim().length() < 1) {
            return null;
        }

        if (bootProps == null) {
            if (homeDir == null) {
                return value;
            } else {
                bootProps = new Properties();
            }
        }

        bootProps.setProperty(getHomeSystemProperty(), homeDir);

        if (interp == null) {
            interp = new StringSearchInterpolator();
            interp.addValueSource(new PropertiesBasedValueSource(bootProps));
        }

        return interp.interpolate(value);
    }

    public boolean isHelp() {
        return help;
    }

    public BootOptions setHelp(final boolean help) {
        this.help = help;
        return this;
    }

    public boolean parseArgs(final String[] args) throws BootException {
        final CmdLineParser parser = new CmdLineParser(this);
        boolean canStart = true;
        try {
            parser.parseArgument(args);
        } catch (final CmdLineException e) {
            throw new BootException("Failed to parse command-line args: %s", e,
                    e.getMessage());
        }

        if (isHelp()) {
            printUsage(parser, null);
            canStart = false;
        }

        return canStart;
    }

    public static void printUsage(final CmdLineParser parser,
            final CmdLineException error) {
        if (error != null) {
            System.err.println("Invalid option(s): " + error.getMessage());
            System.err.println();
        }

        System.err.println("Usage: $0 [OPTIONS] [<target-path>]");
        System.err.println();
        System.err.println();
        // If we are running under a Linux shell COLUMNS might be available for
        // the width
        // of the terminal.
        parser.setUsageWidth(System.getenv("COLUMNS") == null ? 100 : Integer
                .valueOf(System.getenv("COLUMNS")));
        parser.printUsage(System.err);
        System.err.println();
    }

    public String getHomeDir() {
        return homeDir == null ? System.getProperty("user.home") + "/." + getHomeSystemProperty() : homeDir;
    }

    public void setHomeDir(final String home) {
        homeDir = home;
    }

    public String getConfig() {
        return config == null ? new File(getHomeDir(), "." + getHomeSystemProperty() + "/etc/main.conf")
                .getPath() : config;
    }

}
