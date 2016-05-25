package scalaz
package managed

import scalaz.effect._

import java.io._
import java.nio.file._

object Examples {

  def withFileOutputStream[A](p: Path): Forall[Lambda[R => (OutputStream => IO[R]) => IO[R]]] =
    new Forall[Lambda[R => (OutputStream => IO[R]) => IO[R]]] {
      def apply[R]: (OutputStream => IO[R]) => IO[R] = run =>
        IO(Files.newOutputStream(p)).
          bracket(os => IO { println("closing outputstream"); os.close })(run)
    }

  def withFileInputStream[A](p: Path): Forall[Lambda[R => (InputStream => IO[R]) => IO[R]]] =
    new Forall[Lambda[R => (InputStream => IO[R]) => IO[R]]] {
      def apply[R]: (InputStream => IO[R]) => IO[R] = run =>
        IO(Files.newInputStream(p)).
          bracket(is => IO { println("closing inputstream"); is.close })(run)
    }

  def copy(in: InputStream, out: OutputStream): IO[Unit] =
    IO {
      Iterator.continually(in.read).
        takeWhile(_ != -1).
        foreach(out.write)
    }

  def run: IO[Unit] =
    Managed.run(
      for {
        _   <- IO.putStrLn("opening input file").liftIO[Managed]
        in  <- Managed(withFileInputStream(Paths.get("build.sbt")))
        _   <- IO.putStrLn("opening output file").liftIO[Managed]
        out <- Managed(withFileOutputStream(Paths.get("target/output.sbt")))
        _   <- IO.putStrLn("copying").liftIO[Managed]
        _   <- copy(in, out).liftIO[Managed]
        _   <- IO.putStrLn("finished copying").liftIO[Managed]
      } yield ()
    )

  def main(args: Array[String]): Unit =
    run.unsafePerformIO
}

