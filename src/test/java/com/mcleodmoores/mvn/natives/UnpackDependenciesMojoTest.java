/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.mcleodmoores.misc.OutputStreamOpener;
import com.mcleodmoores.mvn.natives.UnpackDependenciesMojo;

/**
 * Tests the {@link UnpackDependenciesMojo} class.
 */
@Test
public class UnpackDependenciesMojoTest {

  public void testGetAndSet () {
    final UnpackDependenciesMojo instance = new UnpackDependenciesMojo ();
    instance.setSkip (true);
    assertTrue (instance.isSkip ());
  }

  private UnpackDependenciesMojo executeInstance (final File tmp, final Set<Artifact> artifacts) {
    final UnpackDependenciesMojo instance = new UnpackDependenciesMojo ();
    instance.setLog (Mockito.mock (Log.class));
    final MavenProject project = new MavenProject ();
    project.getBuild ().setDirectory (tmp.getAbsolutePath ());
    project.setArtifacts (artifacts);
    instance.setPluginContext (Collections.singletonMap ("project", project));
    return instance;
  }

  private static void delete (final File dir) {
    if (dir.isDirectory ()) {
      for (final File file : dir.listFiles ()) {
        delete (file);
      }
    }
    dir.delete ();
  }

  @Test
  public void testExecuteSkip () throws Exception {
    final File tmp = Files.createTempDir ();
    try {
      final UnpackDependenciesMojo instance = executeInstance (tmp,
          Collections.<Artifact> emptySet ());
      instance.setSkip (true);
      instance.execute ();
      assertFalse ((new File (tmp, "dependency")).exists ());
    } finally {
      delete (tmp);
    }
  }

  public void testExecuteNoDependencies () throws Exception {
    final File tmp = Files.createTempDir ();
    try {
      final UnpackDependenciesMojo instance = executeInstance (tmp,
          Collections.<Artifact> emptySet ());
      instance.execute ();
      final File dependency = new File (tmp, "dependency");
      assertTrue (dependency.exists ());
      assertEquals (dependency.list (), ArrayUtils.EMPTY_STRING_ARRAY);
    } finally {
      delete (tmp);
    }
  }

  private static Artifact createArtifact (final File tmp, final String type, final String member)
      throws IOException {
    final File zipFile = new File (tmp, type + ".zip");
    try (final FileOutputStream out = new FileOutputStream (zipFile)) {
      final ZipOutputStream zipStream = new ZipOutputStream (out);
      final ZipEntry license = new ZipEntry ("LICENSE");
      zipStream.putNextEntry (license);
      zipStream.write (26);
      zipStream.closeEntry ();
      final ZipEntry payload = new ZipEntry (member);
      zipStream.putNextEntry (payload);
      zipStream.write (26);
      zipStream.closeEntry ();
      zipStream.close ();
    }
    final Artifact artifact = Mockito.mock (Artifact.class);
    Mockito.when (artifact.getType ()).thenReturn (type);
    Mockito.when (artifact.getGroupId ()).thenReturn ("uk.co.beerdragon");
    Mockito.when (artifact.getArtifactId ()).thenReturn ("test-" + type);
    Mockito.when (artifact.getVersion ()).thenReturn ("SNAPSHOT");
    Mockito.when (artifact.getFile ()).thenReturn (zipFile);
    return artifact;
  }

  public void testExecuteNoRename () throws Exception {
    final File tmp = Files.createTempDir ();
    try {
      final UnpackDependenciesMojo instance = executeInstance (tmp,
          Collections.singleton (createArtifact (tmp, "native-exec", "bin/test.exe")));
      instance.execute ();
      final File dependency = new File (tmp, "dependency");
      assertTrue (dependency.exists ());
      assertTrue ((new File (dependency, "LICENSE")).exists ());
      assertTrue ((new File (dependency, "bin")).exists ());
      assertTrue ((new File (new File (dependency, "bin"), "test.exe")).exists ());
    } finally {
      delete (tmp);
    }
  }

  public void testCreateUniqueName () {
    final UnpackDependenciesMojo instance = new UnpackDependenciesMojo ();
    final Artifact a = new DefaultArtifact ("group-A", "artifact-A", "version-A", "test",
        "native-static", "classifier-A", null);
    final Artifact b = new DefaultArtifact ("group-A", "artifact-A", "version-A", "test",
        "native-static", "", null);
    final Artifact c = new DefaultArtifact ("group-A", "artifact-A", "version-B", "test",
        "native-static", "", null);
    final Artifact d = new DefaultArtifact ("group-B", "artifact-A", "version-A", "test",
        "native-static", "", null);
    final Artifact e = new DefaultArtifact ("group-B", "artifact-A", "version-B", "test",
        "native-static", "", null);
    final Artifact f = new DefaultArtifact ("group-A", "artifact-B", "version-A", "test",
        "native-static", "", null);
    assertEquals (instance.createUniqueName (a, "foo", Collections.singleton (a)), "foo");
    assertEquals (instance.createUniqueName (a, "foo.txt", Collections.singleton (a)), "foo.txt");
    assertEquals (instance.createUniqueName (a, "bar/foo", ImmutableSet.of (a, f)),
        "bar/foo-artifact-A");
    assertEquals (instance.createUniqueName (f, "bar/foo", ImmutableSet.of (a, f)),
        "bar/foo-artifact-B");
    assertEquals (instance.createUniqueName (a, "foo", ImmutableSet.of (a, e)),
        "foo-group-A-artifact-A");
    assertEquals (instance.createUniqueName (e, "foo", ImmutableSet.of (a, e)),
        "foo-group-B-artifact-A");
    assertEquals (instance.createUniqueName (a, "foo", ImmutableSet.of (a, d, e)),
        "foo-group-A-artifact-A_version-A");
    assertEquals (instance.createUniqueName (d, "foo", ImmutableSet.of (a, d, e)),
        "foo-group-B-artifact-A_version-A");
    assertEquals (instance.createUniqueName (e, "foo", ImmutableSet.of (a, d, e)),
        "foo-group-B-artifact-A_version-B");
    assertEquals (instance.createUniqueName (a, "foo.txt", ImmutableSet.of (a, c)),
        "foo-artifact-A-classifier-A.txt");
    assertEquals (instance.createUniqueName (c, "foo.txt", ImmutableSet.of (a, c)),
        "foo-artifact-A.txt");
    assertEquals (instance.createUniqueName (a, "foo", ImmutableSet.of (a, b)),
        "foo-artifact-A-classifier-A");
    assertEquals (instance.createUniqueName (b, "foo", ImmutableSet.of (a, b)), "foo-artifact-A");
  }

  @Test (expectedExceptions = IllegalArgumentException.class)
  public void testInvalidPackaging () {
    final UnpackDependenciesMojo instance = new UnpackDependenciesMojo ();
    final Artifact a = new DefaultArtifact ("group-A", "artifact-A", "version-A", "test",
        "native-static", "", null);
    final Artifact b = new DefaultArtifact ("group-A", "artifact-A", "version-A", "test",
        "native-dynamic", "", null);
    instance.createUniqueName (a, "include/std.h", ImmutableSet.of (a, b));
  }

  public void testExecuteNativeDependencies () throws Exception {
    final File tmp = Files.createTempDir ();
    try {
      final UnpackDependenciesMojo instance = executeInstance (
          tmp,
          ImmutableSet.of (createArtifact (tmp, "native-static", "lib/test.lib"),
              createArtifact (tmp, "native-exec", "bin/test.exe"),
              createArtifact (tmp, "native-dynamic", "bin/test.dll")));
      instance.execute ();
      final File dependency = new File (tmp, "dependency");
      assertTrue (dependency.exists ());
      assertFalse ((new File (dependency, "LICENSE")).exists ());
      assertTrue ((new File (dependency, "LICENSE-test-native-static")).exists ());
      assertTrue ((new File (dependency, "LICENSE-test-native-exec")).exists ());
      assertTrue ((new File (dependency, "LICENSE-test-native-dynamic")).exists ());
      assertTrue ((new File (dependency, "lib")).exists ());
      assertTrue ((new File (new File (dependency, "lib"), "test.lib")).exists ());
      assertTrue ((new File (dependency, "bin")).exists ());
      assertTrue ((new File (new File (dependency, "bin"), "test.exe")).exists ());
      assertTrue ((new File (new File (dependency, "bin"), "test.dll")).exists ());
    } finally {
      delete (tmp);
    }
  }

  @Test (expectedExceptions = MojoFailureException.class)
  public void testExecuteInvalidDependencies () throws Exception {
    final File tmp = Files.createTempDir ();
    try {
      final UnpackDependenciesMojo instance = executeInstance (tmp,
          Collections.singleton (createArtifact (tmp, "native-exec", "test.exe")));
      final File zipFile = new File (tmp, "native-exec.zip");
      zipFile.delete ();
      instance.execute ();
    } finally {
      delete (tmp);
    }
  }

  @Test (expectedExceptions = MojoFailureException.class)
  public void testExecuteInvalidDestination () throws Exception {
    final File tmp = Files.createTempDir ();
    try {
      final UnpackDependenciesMojo instance = executeInstance (tmp,
          Collections.singleton (createArtifact (tmp, "native-exec", "test.exe")));
      (new File ((new File (tmp, "dependency")), "LICENSE")).mkdirs ();
      instance.execute ();
    } finally {
      delete (tmp);
    }
  }

  @Test (expectedExceptions = MojoFailureException.class)
  public void testExecuteWriteFailure () throws Exception {
    final File tmp = Files.createTempDir ();
    try {
      final UnpackDependenciesMojo instance = executeInstance (tmp,
          Collections.singleton (createArtifact (tmp, "native-exec", "test.exe")));
      final OutputStreamOpener outputStreams = Mockito.mock (OutputStreamOpener.class);
      Mockito.when (outputStreams.open (Mockito.<File> any ())).thenReturn (
          new ByteArrayOutputStream () {

            @Override
            public void close () throws IOException {
              throw new IOException ();
            }

          });
      instance.setOutputStreams (outputStreams);
      instance.execute ();
    } finally {
      delete (tmp);
    }
  }

  public void testExecuteJarDependency () throws Exception {
    final File tmp = Files.createTempDir ();
    try {
      final UnpackDependenciesMojo instance = executeInstance (tmp,
          Collections.singleton (createArtifact (tmp, "zip", "classes/A.class")));
      instance.execute ();
      final File dependency = new File (tmp, "dependency");
      assertTrue (dependency.exists ());
      assertEquals (dependency.list (), ArrayUtils.EMPTY_STRING_ARRAY);
    } finally {
      delete (tmp);
    }
  }

}