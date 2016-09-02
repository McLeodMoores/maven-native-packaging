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
 * Specialisation of {@link Source} to describe static library files.
 */
public class StaticLib extends ArchSource {

  /**
   * Header files related to the static library that a consumer will need to link correctly.
   */
  private HeaderFile[] _headers;

  /**
   * Returns the header files related to the static library.
   * 
   * @return sources of header files
   */
  public HeaderFile[] getHeaders () {
    return ArrayUtils.clone (_headers);
  }

  /**
   * Sets the header files related to the static library.
   * 
   * @param headers
   *          sources of header files
   */
  public void setHeaders (final HeaderFile[] headers) {
    _headers = ArrayUtils.clone (headers);
  }

  // ArchSource

  @Override
  /* package */void accept (final SourceVisitor visitor) {
    visitor.visitStaticLib (this);
  }

  @Override
  public int hashCode () {
    return super.hashCode () * 17 + Arrays.deepHashCode (getHeaders ());
  }

  @Override
  /* package */boolean equalsImpl (final Object o) {
    if (!super.equalsImpl (o)) return false;
    final StaticLib other = (StaticLib)o;
    return Objects.deepEquals (getHeaders (), other.getHeaders ());
  }

  @Override
  /* package */void toString (final StringBuilder sb) {
    super.toString (sb);
    if (getHeaders () != null) {
      sb.append (", headers:").append (Arrays.toString (getHeaders ()));
    }
  }

}