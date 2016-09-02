/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives;

import java.util.Objects;

/**
 * Description of a source of files to be packaged as a native build result for a given
 * architecture.
 */
public class ArchSource extends Source {

  /**
   * Architecture descriptor, for example <code>i386</code>.
   */
  private String _arch;

  /**
   * Returns the architecture descriptor, for example <code>i386</code>.
   * 
   * @return the architecture descriptor
   */
  public String getArch () {
    return _arch;
  }

  /**
   * Sets the architecture descriptor, for example <code>i386</code>.
   * 
   * @param arch
   *          the architecture descriptor
   */
  public void setArch (final String arch) {
    _arch = arch;
  }

  // Source

  @Override
  /* package */void accept (final SourceVisitor visitor) {
    visitor.visitArchSource (this);
  }

  @Override
  public int hashCode () {
    return super.hashCode () * 17 + Objects.hashCode (getArch ());
  }

  @Override
  /* package */boolean equalsImpl (final Object o) {
    if (!super.equalsImpl (o)) return false;
    final ArchSource other = (ArchSource)o;
    return Objects.equals (getArch (), other.getArch ());
  }

  @Override
  /* package */void toString (final StringBuilder sb) {
    super.toString (sb);
    if (getArch () != null) {
      sb.append (", arch:").append (getArch ());
    }
  }

}