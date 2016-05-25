package scalaz
package managed

import scalaz.effect._

sealed abstract class Managed[A] { self =>
  protected[Managed] def apply[R](pure_ : A => IO[R]): IO[R]

  final def map[B](f: A => B): Managed[B] =
    new Managed[B] {
      def apply[R](pure_ : B => IO[R]): IO[R] =
        self.apply(a => pure_(f(a)))
    }

  final def flatMap[B](f: A => Managed[B]): Managed[B] =
    new Managed[B] {
      def apply[R](pure_ : B => IO[R]): IO[R] =
        self.apply(a => f(a).apply(b => pure_(b)))
    }
}

object Managed {
  def apply[A](f: Forall[Lambda[R => (A => IO[R]) => IO[R]]]): Managed[A] =
    new Managed[A] {
      def apply[R](pure_ : A => IO[R]): IO[R] =
        f.apply(pure_)
    }

  def run(m: Managed[Unit]): IO[Unit] =
    m(Monad[IO].point(_))

  def withA[A, R](m: Managed[A])(f: A => IO[R]): IO[R] =
    m.apply(f)

  implicit val ManagedMonadIO: MonadIO[Managed] = new MonadIO[Managed] {
    override def point[A](a: => A): Managed[A] =
      new Managed[A] {
        override def apply[R](pure_ : A => IO[R]): IO[R] =
          pure_(a)
      }

    override def map[A, B](fa: Managed[A])(f: A => B): Managed[B] =
      fa.map(f)

    override def bind[A, B](fa: Managed[A])(f: A => Managed[B]): Managed[B] =
      fa.flatMap(f)

    override def liftIO[A](io: IO[A]): Managed[A] =
      new Managed[A] {
        def apply[R](pure_ : A => IO[R]): IO[R] =
          io.flatMap(pure_)
      }
  }

  implicit def ManagedMonoid[A](implicit A: Monoid[A]): Monoid[Managed[A]] =
    new Monoid[Managed[A]] {
      override def zero: Managed[A] = Monad[Managed].point(A.zero)

      override def append(x: Managed[A], y: => Managed[A]): Managed[A] =
        x.flatMap(a => y.map(A.append(a, _)))
    }
}

