/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.mcleodmoores.mvn.natives.defaults.Defaults;

/**
 * Implementation of the {@code package-static} goal.
 * <p>
 * This is a specialised form of {@link PackageMojo} with simplified configuration for the static
 * library packaging use case.
 */
public class PackageStaticMojo extends AbstractMojo {

  private boolean _skip;

  private String _defaults;

  private Source[] _sources;

  private StaticLib[] _staticLibs;

  public boolean isSkip () {
    return _skip;
  }

  public void setSkip (final boolean skip) {
    _skip = skip;
  }

  public String getDefaults () {
    return _defaults;
  }

  public void setDefaults (final String defaults) {
    _defaults = defaults;
  }

  public Source[] getSources () {
    return ArrayUtils.clone (_sources);
  }

  public void setSources (final Source[] sources) {
    _sources = ArrayUtils.clone (sources);
  }

  public StaticLib[] getStaticLibs () {
    return ArrayUtils.clone (_staticLibs);
  }

  public void setStaticLibs (final StaticLib[] staticLibs) {
    _staticLibs = ArrayUtils.clone (staticLibs);
  }

  /* package */void applyDefaults () {
    final Defaults defaults = Defaults.get (getDefaults ());
    defaults.applyTo (getSources ());
    if (getStaticLibs () == null) {
      setStaticLibs (defaults.createDefaultStaticLibs ());
    } else {
      defaults.applyTo (getStaticLibs ());
    }
  }

  /* package */PackageMojo delegate () {
    final PackageMojo delegate = new PackageMojo ();
    delegate.setLog (getLog ());
    delegate.setPluginContext (getPluginContext ());
    delegate.setSkip (isSkip ());
    delegate.setSources (getSources ());
    delegate.setStaticLibs (getStaticLibs ());
    return delegate;
  }

  // Mojo

  @Override
  public void execute () throws MojoExecutionException, MojoFailureException {
    applyDefaults ();
    delegate ().execute ();
  }

}