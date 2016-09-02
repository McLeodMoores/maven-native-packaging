/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Specialisation of {@link Source} to describe executable files.
 */
public class Executable extends ArchSource {

  /**
   * Header files related to the executable that a consumer will need to use it correctly.
   */
  private HeaderFile[] _headers;

  /**
   * Libraries related to the executable that a consumer will need to use it correctly.
   */
  private ArchSource[] _libraries;

  /**
   * Returns the header files related to the executable.
   * 
   * @return sources of header files
   */
  public HeaderFile[] getHeaders () {
    return ArrayUtils.clone (_headers);
  }

  /**
   * Sets the header files related to the executable.
   * 
   * @param headers
   *          sources of header files
   */
  public void setHeaders (final HeaderFile[] headers) {
    _headers = ArrayUtils.clone (headers);
  }

  /**
   * Returns the libraries that can be used to consume the executable.
   * 
   * @return sources of static or dynamic libraries
   */
  public ArchSource[] getLibraries () {
    return ArrayUtils.clone (_libraries);
  }

  /**
   * Sets the libraries that can be used to consume the executable.
   * 
   * @param libraries
   *          sources of static or dynamic libraries
   */
  public void setLibraries (final ArchSource[] libraries) {
    _libraries = ArrayUtils.clone (libraries);
  }

  // ArchSource

  @Override
  /* package */void accept (final SourceVisitor visitor) {
    visitor.visitExecutable (this);
  }

  @Override
  public int hashCode () {
    return super.hashCode () * 17 + Arrays.deepHashCode (getHeaders ()) * 31
        + Arrays.deepHashCode (getLibraries ());
  }

  @Override
  /* package */boolean equalsImpl (final Object o) {
    if (!super.equalsImpl (o)) return false;
    final Executable other = (Executable)o;
    return Objects.deepEquals (getHeaders (), other.getHeaders ())
        && Objects.deepEquals (getLibraries (), other.getLibraries ());
  }

  @Override
  /* package */void toString (final StringBuilder sb) {
    super.toString (sb);
    if (getHeaders () != null) {
      sb.append (", headers:").append (Arrays.toString (getHeaders ()));
    }
    if (getLibraries () != null) {
      sb.append (", libraries:").append (Arrays.toString (getLibraries ()));
    }
  }

}