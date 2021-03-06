import java.io.ByteArrayInputStream

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

import reactivemongo.bson.utils.Converters

import reactivemongo.api.{ Cursor, SerializationPack, WrappedCursor }

import reactivemongo.api.tests.{ Pack, pack, newBuilder }

import reactivemongo.api.gridfs.{ FileToSave, GridFS }

import org.specs2.concurrent.ExecutionEnv

final class GridFSSpec(implicit ee: ExecutionEnv)
  extends org.specs2.mutable.Specification
  with org.specs2.specification.AfterAll {

  "GridFS" title

  sequential

  // ---

  import tests.Common
  import Common.{ timeout, slowTimeout }

  lazy val (db, slowDb) = Common.databases(s"reactivemongo-gridfs-${System identityHashCode this}", Common.connection, Common.slowConnection)

  def afterAll = { db.drop(); () }

  // ---

  "Default connection" should {
    val prefix = s"fs${System identityHashCode db}"

    gridFsSpec(pack: Pack)(GridFS(db, prefix), timeout)
  }

  "Slow connection" should {
    import reactivemongo.api.{ BSONSerializationPack => LegacyPack }
    import reactivemongo.api.collections.bson.BSONCollectionProducer

    val prefix = s"fs${System identityHashCode slowDb}"

    gridFsSpec(LegacyPack)(GridFS(LegacyPack, slowDb, prefix), slowTimeout)
  }

  // ---

  def gridFsSpec[P <: SerializationPack with Singleton](
    pack: P)(gfs: GridFS[pack.type], timeout: FiniteDuration)(implicit ev: scala.reflect.ClassTag[pack.Value]) = {
    type GFile = gfs.ReadFile[pack.Value]
    val builder = newBuilder(pack)
    import builder.{ document, elementProducer => elem, string => str }
    implicit def dw = pack.IdentityWriter

    val filename1 = s"file1-${System identityHashCode gfs}"
    lazy val file1 = gfs.fileToSave(
      Some(filename1), Some("application/file"))

    lazy val content1 = (1 to 100).view.map(_.toByte).toArray

    "not exists before" in {
      gfs.exists must beFalse.awaitFor(timeout)
    }

    "ensure the indexes are ok" in {
      gfs.ensureIndex() must beTrue.await(2, timeout) and {
        gfs.exists must beTrue.awaitFor(timeout)
      } and {
        gfs.ensureIndex() must beFalse.awaitFor(timeout)
      }
    }

    "store a file without a computed MD5" in {
      def in = new ByteArrayInputStream(content1)

      gfs.writeFromInputStream(file1, in).andThen {
        case _ => in.close()
      }.map(_.filename) must beSome(filename1).await(1, timeout)
    }

    val filename2 = s"file2-${System identityHashCode gfs}"
    lazy val file2 = gfs.fileToSave(
      _filename = Some(filename2),
      _contentType = Some("text/plain"),
      _uploadDate = None,
      _metadata = document(Seq(elem("foo", str("bar")))),
      _id = str(filename2))

    lazy val content2 = (100 to 200).view.map(_.toByte).toArray

    "store a file with computed MD5" in {
      def in = new ByteArrayInputStream(content2)

      gfs.writeFromInputStream(file2, in).andThen {
        case _ => in.close()
      }.map(_.filename) must beSome(filename2).await(1, timeout)
    }

    "find the files" in {
      def find(n: String): Future[Option[GFile]] =
        gfs.find[pack.Document, pack.Value](
          document(Seq(elem("filename", str(n))))).headOption

      def matchFile(
        actual: GFile,
        expected: FileToSave[_, _],
        content: Array[Byte]) = actual.filename must_=== expected.filename and {
        actual.uploadDate must beSome
      } and (actual.contentType must_=== expected.contentType) and {
        val buf = new java.io.ByteArrayOutputStream()

        gfs.readToOutputStream(actual, buf).
          map(_ => buf.toByteArray) must beTypedEqualTo(content).
          await(1, timeout) and {
            gfs.chunks(actual).fold(Array.empty[Byte]) { _ ++ _ }.
              aka("chunks") must beTypedEqualTo(content).awaitFor(timeout)
          }
      }

      {
        implicit def fooProducer[T] = new reactivemongo.api.CursorProducer[T] {
          type ProducedCursor = FooCursor[T]

          def produce(base: Cursor.WithOps[T]): ProducedCursor =
            new DefaultFooCursor(base)
        }

        val cursor = gfs.find(document(Seq(elem("filename", str(filename1)))))

        cursor.foo must_=== "Bar" and {
          cursor.headOption must beSome[GFile].
            which(matchFile(_, file1, content1)).await(1, timeout)
        }
      } and {
        find(filename2) aka "file #2" must beSome[GFile].which { actual =>
          def expectedMd5 = Converters.hex2Str(Converters.md5(content2))

          matchFile(actual, file2, content2) and {
            actual.md5 must beSome[String].which {
              _ aka "MD5" must_=== expectedMd5
            }
          }
        }.await(1, timeout)
      }
    }

    "delete the files from GridFS" in {
      (for {
        a <- gfs.remove(file1.id).map(_.n)
        b <- gfs.remove(file2.id).map(_.n)
      } yield a + b) must beTypedEqualTo(2).await(1, timeout)
    }
  }

  // ---

  private sealed trait FooCursor[T] extends Cursor[T] { def foo: String }

  private sealed trait FooExtCursor[T] extends FooCursor[T]

  private class DefaultFooCursor[T](val wrappee: Cursor[T])
    extends FooExtCursor[T] with WrappedCursor[T] {
    val foo = "Bar"
  }
}
