/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.mcleodmoores.misc.ProcessExecutor;
import com.mcleodmoores.mvn.natives.BuildMojo;

/**
 * Tests the {@link BuildMojo} class.
 */
@Test
public class BuildMojoTest {

  public void testGetAndSet () {
    final BuildMojo instance = new BuildMojo ();
    instance.setSkip (true);
    instance.setDefaults ("windows");
    instance.setCommand ("foo");
    assertTrue (instance.isSkip ());
    assertEquals (instance.getDefaults (), "windows");
    assertEquals (instance.getCommand (), "foo");
  }

  public void testDefaultsApplied () {
    final BuildMojo instance = new BuildMojo ();
    instance.setDefaults ("windows");
    instance.applyDefaults ();
    assertFalse (instance.isSkip ());
    assertEquals (instance.getCommand (), "build.bat");
  }

  public void testDefaultsOverridden () {
    final BuildMojo instance = new BuildMojo ();
    instance.setDefaults ("windows");
    instance.setCommand ("foo");
    instance.applyDefaults ();
    assertFalse (instance.isSkip ());
    assertEquals (instance.getCommand (), "foo");
  }

  public void testNoDefaults () {
    final BuildMojo instance = new BuildMojo ();
    instance.applyDefaults ();
    assertFalse (instance.isSkip ());
    assertNull (instance.getCommand ());
  }

  private BuildMojo executeInstance () {
    final BuildMojo instance = new BuildMojo ();
    instance.setCommand ("build.bat");
    instance.setLog (Mockito.mock (Log.class));
    return instance;
  }

  public void testSkipExecute () throws Exception {
    final BuildMojo instance = executeInstance ();
    final ProcessExecutor executor = Mockito.mock (ProcessExecutor.class);
    instance.setExecutor (executor);
    instance.setSkip (true);
    instance.execute ();
    Mockito.verifyZeroInteractions (executor);
  }

  public void testExecute () throws Exception {
    final BuildMojo instance = executeInstance ();
    final ProcessExecutor executor = Mockito.mock (ProcessExecutor.class);
    Mockito.when (executor.exec ("build.bat")).thenReturn (ConcurrentUtils.constantFuture (0));
    instance.setExecutor (executor);
    instance.execute ();
    Mockito.verify (executor).exec ("build.bat");
  }

  @Test (expectedExceptions = MojoFailureException.class)
  public void testExecuteExitCodeFail () throws Exception {
    final BuildMojo instance = executeInstance ();
    final ProcessExecutor executor = Mockito.mock (ProcessExecutor.class);
    Mockito.when (executor.exec ("build.bat")).thenReturn (ConcurrentUtils.constantFuture (1));
    instance.setExecutor (executor);
    instance.execute ();
  }

  @Test (expectedExceptions = MojoFailureException.class)
  public void testExecuteSpawnFail () throws Exception {
    final BuildMojo instance = executeInstance ();
    final ProcessExecutor executor = Mockito.mock (ProcessExecutor.class);
    Mockito.when (executor.exec ("build.bat")).thenThrow (new RuntimeException ());
    instance.setExecutor (executor);
    instance.execute ();
  }

}