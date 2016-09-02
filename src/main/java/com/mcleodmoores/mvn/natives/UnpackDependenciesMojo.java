/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.mcleodmoores.misc.IOCallback;
import com.mcleodmoores.misc.OutputStreamOpener;
import com.mcleodmoores.misc.IOCallback.IOExceptionHandler;

/**
 * Implementation of the {@code unpack-dependencies} goal.
 */
public class UnpackDependenciesMojo extends AbstractMojo {

  private OutputStreamOpener _outputStreams = new OutputStreamOpener ();

  private boolean _skip;

  /* package */OutputStreamOpener getOutputStreams () {
    return _outputStreams;
  }

  /* package */void setOutputStreams (final OutputStreamOpener outputStreams) {
    _outputStreams = Objects.requireNonNull (outputStreams);
  }

  public boolean isSkip () {
    return _skip;
  }

  public void setSkip (final boolean skip) {
    _skip = skip;
  }

  private boolean isNative (final Artifact artifact) {
    final String type = artifact.getType ();
    return "native-static".equals (type) || "native-exec".equals (type)
        || "native-dynamic".equals (type);
  }

  private InputStream open (final Artifact artifact) throws MojoFailureException {
    try {
      return new FileInputStream (artifact.getFile ());
    } catch (final IOException e) {
      throw new MojoFailureException ("Can't read from artifact " + ArtifactUtils.key (artifact));
    }
  }

  private void check (final Artifact artifact, final Boolean result) throws MojoFailureException {
    if (result != Boolean.TRUE) {
      throw new MojoFailureException ("Error unpacking " + ArtifactUtils.key (artifact));
    }
  }

  private void gatherName (final Artifact scope, final String name,
      final Map<String, Set<Artifact>> names) {
    Set<Artifact> collision = names.get (name);
    if (collision == null) {
      names.put (name, Collections.<Artifact> singleton (scope));
    } else {
      if (collision.size () == 1) {
        collision = new HashSet<Artifact> (collision);
        names.put (name, collision);
      }
      collision.add (scope);
    }
  }

  private void gatherNames (final Artifact artifact, final Map<String, Set<Artifact>> names)
      throws MojoFailureException {
    getLog ().debug ("Scanning " + ArtifactUtils.key (artifact));
    check (artifact, (new IOCallback<InputStream, Boolean> (open (artifact)) {

      @Override
      protected Boolean apply (final InputStream input) throws IOException {
        final ZipInputStream zip = new ZipInputStream (new BufferedInputStream (input));
        ZipEntry entry;
        while ((entry = zip.getNextEntry ()) != null) {
          gatherName (artifact, entry.getName (), names);
        }
        return Boolean.TRUE;
      }

    }).call (new MojoLoggingErrorCallback (this)));
  }

  private interface ArtifactQuery {

    String get (Artifact artifact);

  }

  private static boolean isUnique (final Collection<Artifact> artifacts, final ArtifactQuery query) {
    final Set<String> values = new HashSet<String> ();
    for (final Artifact artifact : artifacts) {
      if (values.add (query.get (artifact)) == false) {
        return false;
      }
    }
    return true;
  }

  public static boolean isIdentical (final Collection<Artifact> artifacts, final ArtifactQuery query) {
    final Set<String> values = new HashSet<String> ();
    for (final Artifact artifact : artifacts) {
      if (values.add (query.get (artifact)) && (values.size () > 1)) {
        return false;
      }
    }
    return true;
  }

  private static String createUniqueName (final String name, final String suffix) {
    final int dot = name.lastIndexOf ('.');
    if (dot <= 0) {
      return name + suffix;
    } else {
      return name.substring (0, dot) + suffix + name.substring (dot);
    }
  }

  private static String classifierSuffix (final Artifact artifact) {
    if (StringUtils.isEmpty (artifact.getClassifier ())) {
      return "";
    } else {
      return "-" + artifact.getClassifier ();
    }
  }

  /* package */String createUniqueName (final Artifact artifact, final String name,
      final Set<Artifact> artifacts) {
    if (artifacts.size () == 1) {
      return name;
    } else {
      String suffix = "-" + artifact.getArtifactId ();
      // Check if artifact-id alone is sufficient
      if (isUnique (artifacts, new ArtifactQuery () {

        @Override
        public String get (final Artifact artifact) {
          return artifact.getArtifactId ();
        }

      })) {
        return createUniqueName (name, suffix);
      }
      // Check if group/artifact is sufficient
      if (isUnique (artifacts, new ArtifactQuery () {

        @Override
        public String get (final Artifact artifact) {
          return artifact.getGroupId () + "-" + artifact.getArtifactId ();
        }

      })) {
        return createUniqueName (name, "-" + artifact.getGroupId () + suffix);
      }
      // Check if there are different group-ids
      if (!isIdentical (artifacts, new ArtifactQuery () {

        @Override
        public String get (final Artifact artifact) {
          return artifact.getGroupId ();
        }

      })) {
        suffix = "-" + artifact.getGroupId () + suffix;
      }
      // Check if the classifier is sufficient
      if (isUnique (artifacts, new ArtifactQuery () {

        @Override
        public String get (final Artifact artifact) {
          return artifact.getGroupId () + "-" + artifact.getArtifactId ()
              + classifierSuffix (artifact);
        }

      })) {
        return createUniqueName (name, suffix + classifierSuffix (artifact));
      }
      // Check if there are different classifiers
      if (!isUnique (artifacts, new ArtifactQuery () {

        @Override
        public String get (final Artifact artifact) {
          return artifact.getGroupId () + "-" + artifact.getArtifactId () + "_"
              + artifact.getVersion ();
        }

      })) {
        suffix = suffix + classifierSuffix (artifact);
      }
      // Check if the version is sufficient
      if (isUnique (artifacts, new ArtifactQuery () {

        @Override
        public String get (final Artifact artifact) {
          return artifact.getGroupId () + "-" + artifact.getArtifactId ()
              + classifierSuffix (artifact) + "_" + artifact.getVersion ();
        }

      })) {
        return createUniqueName (name, suffix + "_" + artifact.getVersion ());
      }
      // Give up
      throw new IllegalArgumentException ();
    }
  }

  private void unpack (final Artifact artifact, final Map<String, Set<Artifact>> names,
      final File targetDir) throws MojoFailureException {
    getLog ().info ("Unpacking " + ArtifactUtils.key (artifact));
    final IOExceptionHandler errorLog = new MojoLoggingErrorCallback (this);
    check (artifact, (new IOCallback<InputStream, Boolean> (open (artifact)) {

      @Override
      protected Boolean apply (final InputStream input) throws IOException {
        final byte[] buffer = new byte[4096];
        final ZipInputStream zip = new ZipInputStream (new BufferedInputStream (input));
        ZipEntry entry;
        while ((entry = zip.getNextEntry ()) != null) {
          final String dest = createUniqueName (artifact, entry.getName (),
              names.get (entry.getName ()));
          getLog ().debug ("Writing " + entry.getName () + " as " + dest);
          File targetFile = targetDir;
          for (final String component : dest.split ("/")) {
            targetFile.mkdir ();
            targetFile = new File (targetFile, component);
          }
          if ((new IOCallback<OutputStream, Boolean> (getOutputStreams ().open (targetFile)) {

            @Override
            protected Boolean apply (final OutputStream output) throws IOException {
              int bytes;
              while ((bytes = zip.read (buffer, 0, buffer.length)) > 0) {
                output.write (buffer, 0, bytes);
              }
              output.close ();
              return Boolean.TRUE;
            }

          }).call (errorLog) != Boolean.TRUE) {
            return Boolean.FALSE;
          }
        }
        return Boolean.TRUE;
      }

    }).call (errorLog));
  }

  // Mojo

  @Override
  public void execute () throws MojoExecutionException, MojoFailureException {
    if (isSkip ()) {
      getLog ().debug ("Skipping step");
      return;
    }
    final MavenProject project = (MavenProject)getPluginContext ().get ("project");
    final File targetDir = new File (new File (project.getBuild ().getDirectory ()), "dependency");
    targetDir.mkdirs ();
    final Map<String, Set<Artifact>> names = new HashMap<String, Set<Artifact>> ();
    for (final Artifact artifact : project.getArtifacts ()) {
      if (isNative (artifact)) {
        gatherNames (artifact, names);
      }
    }
    for (final Artifact artifact : project.getArtifacts ()) {
      if (isNative (artifact)) {
        unpack (artifact, names, targetDir);
      }
    }
  }

}