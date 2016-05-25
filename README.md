# scalaz-managed v0.1

This library contains the `Managed` monad, which is a small building block for
wrapping resources that you acquire in an exception-safe way using a callback.

The `Managed` type is really simple: it's a wrapper for functions that look
like `(A => IO[R]) => IO[R]`.

## Quick Example

The following small program copies `"inFile.txt"` to
`"outFile.txt"`:

```scala
import scalaz._
import scalaz.effect._
import scalaz.managed._

import java.io._
import java.nio.file._

object Examples extends SafeApp {

  def withFileOutputStream[A](p: Path): Forall[Lambda[R => (OutputStream => IO[R]) => IO[R]]] =
    new Forall[Lambda[R => (OutputStream => IO[R]) => IO[R]]] {
      def apply[R]: (OutputStream => IO[R]) => IO[R] = run =>
        IO(Files.newOutputStream(p)).
          bracket(os => IO(os.close))(run)
    }

  def withFileInputStream[A](p: Path): Forall[Lambda[R => (InputStream => IO[R]) => IO[R]]] =
    new Forall[Lambda[R => (InputStream => IO[R]) => IO[R]]] {
      def apply[R]: (InputStream => IO[R]) => IO[R] = run =>
        IO(Files.newInputStream(p)).
          bracket(is => IO(is.close))(run)
    }

  def copy(in: InputStream, out: OutputStream): IO[Unit] =
    IO {
      Iterator.continually(in.read).
        takeWhile(_ != -1).
        foreach(out.write)
    }

  override def runc: IO[Unit] =
    Managed.run(
      for {
        in  <- Managed(withFileInputStream(Paths.get("inFile.txt")))
        out <- Managed(withFileOutputStream(Paths.get("outFile.txt")))
        _   <- copy(in, out).liftIO[Managed]
      } yield ()
    )
}
```

Read the documentation on the `scalaz.managed.Managed` type to learn more about
how to use the `Managed` type.

