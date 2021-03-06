package reactivemongo.api.indexes

import reactivemongo.bson.BSONDocument

import reactivemongo.api.SerializationPack

/**
 * A MongoDB index (excluding the namespace).
 *
 * Consider reading [[http://www.mongodb.org/display/DOCS/Indexes the documentation about indexes in MongoDB]].
 *
 * @param key The index key (it can be composed of multiple fields). This list should not be empty!
 * @param name The name of this index. If you provide none, a name will be computed for you.
 * @param unique Enforces uniqueness.
 * @param background States if this index should be built in background. You should read [[http://www.mongodb.org/display/DOCS/Indexes#Indexes-background%3Atrue the documentation about background indexing]] before using it.
 * @param dropDups States if duplicates should be discarded (if unique = true). Warning: you should read [[http://www.mongodb.org/display/DOCS/Indexes#Indexes-dropDups%3Atrue the documentation]].
 * @param sparse States if the index to build should only consider the documents that have the indexed fields. See [[http://www.mongodb.org/display/DOCS/Indexes#Indexes-sparse%3Atrue the documentation]] on the consequences of such an index.
 * @param version Indicates the [[http://www.mongodb.org/display/DOCS/Index+Versions version]] of the index (1 for >= 2.0, else 0). You should let MongoDB decide.
 * @param options Optional parameters for this index (typically specific to an IndexType like Geo2D).
 * @param partialFilter Optional [[https://docs.mongodb.com/manual/core/index-partial/#partial-index-with-unique-constraints partial filter]] (since MongoDB 3.2)
 */
sealed abstract class Index extends Product with Serializable {
  type Pack <: SerializationPack
  val pack: Pack

  // TODO: Remove impl
  def key: Seq[(String, IndexType)] = Seq.empty

  def name: Option[String] = None

  def unique: Boolean = false

  def background: Boolean = false

  @deprecated("Since MongoDB 2.6", "0.19.1")
  def dropDups: Boolean = false

  def sparse: Boolean = false

  def version: Option[Int] = None

  // TODO: storageEngine (new for Mongo3)

  private[api] def partialFilterDocument: Option[pack.Document]

  private[api] def optionDocument: pack.Document

  @deprecated("Will be internal", "0.19.0")
  def partialFilter: Option[BSONDocument] = partialFilterDocument.flatMap {
    pack.bsonValue(_) match {
      case doc: BSONDocument =>
        Some(doc)

      case _ =>
        None
    }
  }

  @deprecated("Will be internal", "0.19.0")
  def options: BSONDocument = pack.bsonValue(optionDocument) match {
    case doc: BSONDocument => doc
    case _                 => BSONDocument.empty
  }

  /** The name of the index (a default one is computed if none). */
  lazy val eventualName: String = name.getOrElse(key.foldLeft("") {
    (name, kv) =>
      name + (if (name.length > 0) "_" else "") + kv._1 + "_" + kv._2.valueStr
  })

  @deprecated("No longer a case class", "0.19.1")
  val productArity = 9

  def productElement(n: Int): Any = tupled.productElement(n)

  private[api] lazy val tupled = Tuple9(key, name, unique, background, dropDups, sparse, version, partialFilterDocument, optionDocument)

  // TODO: storageEngine (new for Mongo3)

  override def canEqual(that: Any): Boolean = that match {
    case _: Index => true
    case _        => false
  }

  override def equals(that: Any): Boolean = that match {
    case other: Index => tupled == other.tupled
    case _            => false
  }

  override def hashCode: Int = tupled.hashCode
}

object Index { //extends scala.runtime.AbstractFunction9[Seq[(String, IndexType)], Option[String], Boolean, Boolean, Boolean, Boolean, Option[Int], Option[BSONDocument], BSONDocument, Index] {
  import reactivemongo.api.BSONSerializationPack

  type Aux[P] = Index { type Pack = P }

  @deprecated("Use constructor with pack parameter", "0.19.1")
  def apply(
    key: Seq[(String, IndexType)],
    name: Option[String] = None,
    unique: Boolean = false,
    background: Boolean = false,
    @deprecated("Since MongoDB 2.6", "0.19.1") dropDups: Boolean = false,
    sparse: Boolean = false,
    version: Option[Int] = None, // let MongoDB decide
    @deprecated("Will be internal", "0.19.0") partialFilter: Option[BSONDocument] = None,
    @deprecated("Will be internal", "0.19.0") options: BSONDocument = BSONDocument.empty): Index = apply(BSONSerializationPack)(key, name, unique, background, dropDups, sparse, version, partialFilter, options)

  def apply[P <: SerializationPack](_pack: P)(
    key: Seq[(String, IndexType)],
    name: Option[String],
    unique: Boolean,
    background: Boolean,
    @deprecated("Since MongoDB 2.6", "0.19.1") dropDups: Boolean,
    sparse: Boolean,
    version: Option[Int],
    partialFilter: Option[_pack.Document],
    options: _pack.Document): Index.Aux[_pack.type] = {
    def k = key
    def n = name
    def u = unique
    def b = background
    def d = dropDups
    def s = sparse
    def v = version
    def pf = partialFilter
    def o = options

    new Index {
      type Pack = _pack.type
      val pack: Pack = _pack

      override val key = k
      override val name = n
      override val unique = u
      override val background = b
      override val dropDups = d
      override val sparse = s
      override val version = v
      val partialFilterDocument = pf
      val optionDocument = o
    }
  }

  @deprecated("No longer a case class", "0.19.1")
  def unapply(index: Index): Option[Tuple9[Seq[(String, IndexType)], Option[String], Boolean, Boolean, Boolean, Boolean, Option[Int], Option[BSONDocument], BSONDocument]] = Option(index).map { i =>
    Tuple9(i.key, i.name, i.unique, i.background, i.dropDups,
      i.sparse, i.version, i.partialFilter, i.options)
  }

  /** '''EXPERIMENTAL:''' API may change */
  object Key {
    def unapplySeq(index: Index): Option[Seq[(String, IndexType)]] =
      Option(index).map(_.key)
  }
}

/**
 * A MongoDB namespaced index.
 * A MongoDB index is composed with the namespace (the fully qualified collection name) and the other fields of [[reactivemongo.api.indexes.Index]].
 *
 * Consider reading [[http://www.mongodb.org/display/DOCS/Indexes the documentation about indexes in MongoDB]].
 *
 * @param namespace The fully qualified name of the indexed collection.
 * @param index The other fields of the index.
 */
case class NSIndex(namespace: String, index: Index) {
  val (dbName: String, collectionName: String) = {
    val spanned = namespace.span(_ != '.')
    spanned._1 -> spanned._2.drop(1)
  }
}
