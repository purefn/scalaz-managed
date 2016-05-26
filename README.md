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

  def withFileOutputStream[A](p: Path)(f: OutputStream => IO[R]): IO[R] =
    IO(Files.newOutputStream(p)).bracket(os => IO(os.close))(f)

  def withFileInputStream[A](p: Path)(f: InputStream => IO[R]): IO[R] =
    IO(Files.newInputStream(p)).bracket(is => IO(is.close))(f)

  def fileOutputStream(p: Path): Managed[OutputStream] =
    new Managed[OutputStream] {
      def apply[A](f: OutputStream => IO[A]) =
        withFileOutputStream(p)(f)

  def fileInputStream(p: Path): Managed[InputStream] =
    new Managed[InputStream] {
      def apply[A](f: InputStream => IO[A]) =
        withFileInputStream(p)(f)
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
        in  <- fileInputStream(Paths.get("inFile.txt"))
        out <- fileOutputStream(Paths.get("outFile.txt"))
        _   <- copy(in, out).liftIO[Managed]
      } yield ()
    )
}
```

Read the documentation on the `scalaz.managed.Managed` type to learn more about
how to use the `Managed` type.

