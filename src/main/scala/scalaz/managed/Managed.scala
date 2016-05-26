package scalaz
package managed

import scalaz.effect._

/**
 * An example Scala program to copy data from one handle to another might
 * look like this:
 *
 * {{{
 * def main(args: Array[String]) =
 *   withFileInputStream("inFile.txt")(is =>
 *     withFileOutputStream("outFile.txt")(os =>
 *       copy(is, os)
 *     )
 *   )
 *
 * // A hypothetical function for safely using a FileInputStream
 * def withFileInputStream[A](path: String)(f: FileInputStream => IO[A]): IO[A]
 *
 * // A hypothetical function for safely using a FileOutputStream
 * def withFileOutputStream[A](path: String)(f: FileOutputStream => IO[A]): IO[A]
 *
 * // A hypothetical function that copies data from an InputStream to an OutputStream
 * def copy(is: InputStream, os: OutputStream): IO[Unit]
 * }}}
 *
 * `withFileInputStream` and `withFileOutputStream` are a few of many functions
 * that acquire some resource in an exception-safe way.  These functions take a
 * callback function as an argument and they invoke the callback on the resource
 * when it becomes available, guaranteeing that the resource is properly disposed
 * if the callback throws an exception. These functions usually have a type that
 * ends with the following pattern:
 *
 * {{{
 *                       Callback
 *                     -------------
 * def withXXX[A](...)(f: R => IO[A]): IO[A]
 * }}}
 *
 * Here are some examples other examples of this pattern:
 *
 * {{{
 * def withSocket[A](addr: SocketAddress)(f: Socket => IO[A]): IO[A]
 * def withDB[A](config: DBConfig)(f: DB => IO[A]): IO[A]
 * }}}
 *
 * Acquiring multiple resources in this way requires nesting callbacks.
 * However, you can wrap anything of the form `A => IO[R] => IO[R]` in the
 * `Managed` monad, which translates binds to callbacks for you:
 *
 * {{{
 * import scalaz.managed._
 *
 * def fileInputStream(path: String): Managed[FileInputStream] =
 *   new Managed[FileInputStream] {
 *     def apply[A](f: FileInputStream => IO[A]) =
 *       withFileInputStream(path)(f)
 *   }
 *
 * def fileOutputStream(path: String): Managed[FileOutputStream] =
 *   new Managed[FileOutputStream] {
 *     def apply[A](f: FileOutputStream) =
 *       withFileOutputStream(path)(f)
 *   }
 *
 *  def main(args: Array[String]) =
 *    Managed.run(
 *      for {
 *        in  <- fileInputStream("inFile.txt")
 *        out <- fileOutputStream("outFile.txt")
 *        _   <- copy(in, out).liftIO[Managed]
 *      } yield ()
 *    )
 * }}}
 */
abstract class Managed[A] { self =>
  def apply[R](f: A => IO[R]): IO[R]

  final def map[B](f: A => B): Managed[B] =
    new Managed[B] {
      def apply[R](g: B => IO[R]): IO[R] =
        self.apply(a => g(f(a)))
    }

  final def flatMap[B](f: A => Managed[B]): Managed[B] =
    new Managed[B] {
      def apply[R](g: B => IO[R]): IO[R] =
        self.apply(a => f(a).apply(g))
    }
}

object Managed {
  def apply[A](f: Forall[Lambda[R => (A => IO[R]) => IO[R]]]): Managed[A] =
    new Managed[A] {
      def apply[R](g: A => IO[R]): IO[R] = f.apply(g)
    }

  /**
   * Run a `Managed` computation, enforcing that no acquired resources leak
   */
  def run(m: Managed[Unit]): IO[Unit] =
    m(Monad[IO].point(_))

  /**
   * Acquire a `Managed` value
   */
  def withA[A, R](m: Managed[A])(f: A => IO[R]): IO[R] =
    m.apply(f)

  implicit val ManagedMonadIO: MonadIO[Managed] = new MonadIO[Managed] {
    override def point[A](a: => A): Managed[A] =
      new Managed[A] {
        override def apply[R](f: A => IO[R]): IO[R] = f(a)
      }

    override def map[A, B](fa: Managed[A])(f: A => B): Managed[B] =
      fa.map(f)

    override def bind[A, B](fa: Managed[A])(f: A => Managed[B]): Managed[B] =
      fa.flatMap(f)

    override def liftIO[A](io: IO[A]): Managed[A] =
      new Managed[A] {
        def apply[R](f: A => IO[R]): IO[R] = io.flatMap(f)
      }
  }

  implicit def ManagedMonoid[A](implicit A: Monoid[A]): Monoid[Managed[A]] =
    new Monoid[Managed[A]] {
      override def zero: Managed[A] = Monad[Managed].point(A.zero)

      override def append(x: Managed[A], y: => Managed[A]): Managed[A] =
        x.flatMap(a => y.map(A.append(a, _)))
    }
}

