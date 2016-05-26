package scalaz
package managed

import scalaz.effect._

import java.io._
import java.nio.file._

object Examples extends SafeApp {
  def withFileOutputStream[A](p: Path)(f: OutputStream => IO[A]): IO[A] =
    IO(Files.newOutputStream(p)).bracket(os => IO(os.close))(f)

  def fileOutputStream(p: Path): Managed[OutputStream] =
    new Managed[OutputStream] {
      def apply[R](f: OutputStream => IO[R]): IO[R] =
        withFileOutputStream(p)(f)
    }

  def withFileInputStream[A](p: Path)(f: InputStream => IO[A]): IO[A] =
    IO(Files.newInputStream(p)).bracket(is => IO(is.close))(f)

  def fileInputStream[A](p: Path): Managed[InputStream] =
    new Managed[InputStream] {
      def apply[R](f: InputStream => IO[R]): IO[R] =
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
        in  <- fileInputStream(Paths.get("build.sbt"))
        out <- fileOutputStream(Paths.get("target/output.sbt"))
        _   <- copy(in, out).liftIO[Managed]
      } yield ()
    )
}

