/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.misc;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper for {@link Runtime#exec} to simplify code coverage reporting and avoid actually spawning
 * processes during tests.
 */
public class ProcessExecutor {

  private static class ExceptionFuture implements Future<Integer> {

    private final Exception _exception;

    public ExceptionFuture (final Exception exception) {
      _exception = exception;
    }

    @Override
    public boolean cancel (final boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled () {
      return false;
    }

    @Override
    public boolean isDone () {
      return true;
    }

    @Override
    public Integer get () throws InterruptedException, ExecutionException {
      throw new ExecutionException (_exception);
    }

    @Override
    public Integer get (final long timeout, final TimeUnit unit) throws InterruptedException,
        ExecutionException, TimeoutException {
      throw new ExecutionException (_exception);
    }

  }

  private static class ProcessFuture implements Future<Integer> {

    private final Process _process;

    public ProcessFuture (final Process process) {
      _process = process;
    }

    @Override
    public boolean cancel (final boolean mayInterruptIfRunning) {
      throw new UnsupportedOperationException ("TODO");
    }

    @Override
    public boolean isCancelled () {
      return false;
    }

    @Override
    public boolean isDone () {
      try {
        _process.exitValue();
      } catch (final IllegalThreadStateException e) {
        return false;
      }
      return true;
    }

    private void sleep() {
      try {
        Thread.sleep(10);
      } catch (final InterruptedException e2) {
      }
    }
    @Override
    public Integer get () throws InterruptedException, ExecutionException {
      InputStream inputStream = _process.getInputStream();
      InputStream errorStream = _process.getErrorStream();
      try {
        byte[] buffer = new byte[4096];
        while (!isDone()) {
          int inputBytesRead = inputStream.available() > 0 ? inputStream.read(buffer) : -1;
          if (inputBytesRead != -1) {
            System.out.write(buffer, 0, inputBytesRead);
          } else {
            sleep();
          }

          int errorBytesRead = errorStream.available() > 0 ? errorStream.read(buffer) : -1;
          if (errorBytesRead != -1) {
            System.err.write(buffer, 0, errorBytesRead);
          } else {
            // try not to spin on isDone() too heavily if process busy
            sleep();
          }
          if (inputBytesRead == -1 && errorBytesRead == -1) {
            sleep();
          }
        }
      } catch (IOException ioe) {
      }
      return _process.waitFor ();
    }

    @Override
    public Integer get (final long timeout, final TimeUnit unit) throws InterruptedException,
        ExecutionException, TimeoutException {
      throw new UnsupportedOperationException ("TODO");
    }

  }

  /**
   * Calls {@link Runtime#exec(String)} and returns a {@link Future} that can be used to wait for
   * the process to complete.
   * 
   * @param command
   *          the command to execute, not {@code null}
   * @return a future that can be used to wait for process completion, or receive any exceptions
   */
  public Future<Integer> exec (final String command) {
    try {
      final Process process = Runtime.getRuntime ().exec (command);
      return new ProcessFuture (process);
    } catch (final Exception e) {
      return new ExceptionFuture (e);
    }
  }

}