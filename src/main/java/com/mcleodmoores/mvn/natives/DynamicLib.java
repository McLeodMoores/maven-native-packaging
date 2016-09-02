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
 * Specialisation of {@link Source} to describe dynamic library files.
 */
public class DynamicLib extends ArchSource {

  /**
   * Header files related to the dynamic library that a consumer will need to use it correctly.
   */
  private HeaderFile[] _headers;

  /**
   * Static libraries needed to link against the dynamic library.
   */
  private StaticLib[] _implibs;

  /**
   * Returns the header files related to the dynamic library.
   * 
   * @return sources of header files
   */
  public HeaderFile[] getHeaders () {
    return ArrayUtils.clone (_headers);
  }

  /**
   * Sets the header files related to the dynamic library.
   * 
   * @param headers
   *          sources of header files
   */
  public void setHeaders (final HeaderFile[] headers) {
    _headers = ArrayUtils.clone (headers);
  }

  /**
   * Returns the static libraries that can be used to consume the dynamic library.
   * 
   * @return sources of static import libraries
   */
  public StaticLib[] getImplibs () {
    return ArrayUtils.clone (_implibs);
  }

  /**
   * Sets the static libraries that can be used to consume the dynamic library.
   * 
   * @param implibs
   *          sources of static import libraries
   */
  public void setImplibs (final StaticLib[] implibs) {
    _implibs = ArrayUtils.clone (implibs);
  }

  // ArchSource

  @Override
  /* package */void accept (final SourceVisitor visitor) {
    visitor.visitDynamicLib (this);
  }

  @Override
  public int hashCode () {
    return super.hashCode () * 17 + Arrays.deepHashCode (getHeaders ()) * 31
        + Arrays.deepHashCode (getImplibs ());
  }

  @Override
  /* package */boolean equalsImpl (final Object o) {
    if (!super.equalsImpl (o)) return false;
    final DynamicLib other = (DynamicLib)o;
    return Objects.deepEquals (getHeaders (), other.getHeaders ())
        && Objects.deepEquals (getImplibs (), other.getImplibs ());
  }

  @Override
  /* package */void toString (final StringBuilder sb) {
    super.toString (sb);
    if (getHeaders () != null) {
      sb.append (", headers:").append (Arrays.toString (getHeaders ()));
    }
    if (getImplibs () != null) {
      sb.append (", implibs:").append (Arrays.toString (getImplibs ()));
    }
  }

}