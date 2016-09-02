/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives;

import java.util.Objects;
import java.util.concurrent.Future;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.mcleodmoores.misc.ProcessExecutor;
import com.mcleodmoores.mvn.natives.defaults.Defaults;

/**
 * Implementation of the {@code build} goal.
 */
public class BuildMojo extends AbstractMojo {

  private ProcessExecutor _executor = new ProcessExecutor ();

  private boolean _skip;

  private String _defaults;

  private String _command;

  /* package */ProcessExecutor getExecutor () {
    return _executor;
  }

  /* package */void setExecutor (final ProcessExecutor executor) {
    _executor = Objects.requireNonNull (executor);
  }

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

  public String getCommand () {
    return _command;
  }

  public void setCommand (final String command) {
    _command = command;
  }

  /* package */void applyDefaults () {
    final Defaults defaults = Defaults.get (getDefaults ());
    if (getCommand () == null) {
      setCommand (defaults.getDefaultBuildCommand ());
    }
  }

  private int runCommand () throws MojoFailureException {
    try {
      getLog ().info ("Building project with '" + getCommand () + "'");
      final Future<Integer> future = getExecutor ().exec (getCommand ());
      getLog ().debug ("Waiting for external process");
      final int ec = future.get ();
      getLog ().debug ("Process terminated with code " + ec);
      return ec;
    } catch (final Exception e) {
      getLog ().error (e);
      throw new MojoFailureException ("Couldn't execute build command '" + getCommand () + "'");
    }
  }

  // Mojo

  @Override
  public void execute () throws MojoExecutionException, MojoFailureException {
    if (isSkip ()) {
      getLog ().debug ("Skipping step");
      return;
    }
    applyDefaults ();
    final int ec = runCommand ();
    if (ec != 0) throw new MojoFailureException ("Build failed with exit code " + ec);
  }

}