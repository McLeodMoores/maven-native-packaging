/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper for {@link FileInputStream} to simplify code coverage reporting and avoid working with
 * real files during tests.
 */
public class InputStreamOpener {

  /**
   * Opens a file for reading.
   * 
   * @param file
   *          the file to read, not {@code null}
   * @return the input stream, never {@code null}
   * @throws IOException
   *           if the input stream couldn't be opened
   */
  public InputStream open (final File file) throws IOException {
    return new FileInputStream (file);
  }

}