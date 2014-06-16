/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package uk.co.beerdragon.misc;

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
        _process.exitValue ();
      } catch (final IllegalThreadStateException e) {
        return false;
      }
      return true;
    }

    @Override
    public Integer get () throws InterruptedException, ExecutionException {
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