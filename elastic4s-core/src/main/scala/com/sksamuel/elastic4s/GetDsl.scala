package com.sksamuel.elastic4s

import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.client.{Client, Requests}
import org.elasticsearch.index.VersionType
import org.elasticsearch.index.get.GetField
import org.elasticsearch.search.fetch.source.FetchSourceContext
import org.scalactic.{ErrorMessage, Or}

import scala.concurrent.Future
import scala.language.implicitConversions

/** @author Stephen Samuel */
trait GetDsl {

  class GetWithIdExpectsFrom(id: String) {
    def from(index: String, `type`: String): GetDefinition = GetDefinition(IndexAndTypes(index, `type`), id)
    def from(index: IndexAndTypes): GetDefinition = GetDefinition(index, id)
  }

  implicit object GetDefinitionExecutable extends Executable[GetDefinition, GetResponse, RichGetResponse] {
    override def apply(c: Client, t: GetDefinition): Future[RichGetResponse] = {
      injectFutureAndMap(c.get(t.build, _))(RichGetResponse)
    }
  }
}

case class GetDefinition(indexTypes: IndexAndTypes, id: String) {

  private val _builder = Requests.getRequest(indexTypes.index).`type`(indexTypes.types.headOption.orNull).id(id)
  def build = _builder

  def fetchSourceContext(context: Boolean) = {
    _builder.fetchSourceContext(new FetchSourceContext(context))
    this
  }

  def fetchSourceContext(include: Iterable[String], exclude: Iterable[String] = Nil) = {
    _builder.fetchSourceContext(new FetchSourceContext(include.toArray, exclude.toArray))
    this
  }

  def fetchSourceContext(context: FetchSourceContext) = {
    _builder.fetchSourceContext(context)
    this
  }

  def fields(fs: String*): GetDefinition = fields(fs)
  def fields(fs: Iterable[String]): GetDefinition = {
    _builder.fields(fs.toSeq: _*)
    this
  }

  def ignoreErrorsOnGeneratedFields(ignoreErrorsOnGeneratedFields: Boolean) = {
    _builder.ignoreErrorsOnGeneratedFields(ignoreErrorsOnGeneratedFields)
    this
  }

  def parent(p: String) = {
    _builder.parent(p)
    this
  }

  def preference(pref: Preference): GetDefinition = preference(pref.elastic)
  def preference(pref: String): GetDefinition = {
    _builder.preference(pref)
    this
  }

  def realtime(r: Boolean) = {
    _builder.realtime(r)
    this
  }

  def refresh(refresh: Boolean) = {
    _builder.refresh(refresh)
    this
  }

  def routing(r: String) = {
    _builder.routing(r)
    this
  }

  def version(version: Long) = {
    _builder.version(version)
    this
  }

  def versionType(versionType: VersionType) = {
    _builder.versionType(versionType)
    this
  }
}

case class RichGetResponse(original: GetResponse) {

  import scala.collection.JavaConverters._

  // java method aliases
  def getField(name: String): GetField = field(name)
  def getFields = original.getFields
  def getId: String = id
  def getIndex: String = index
  def getType: String = `type`
  def getVersion: Long = version

  def field(name: String): GetField = original.getField(name)
  def fieldOpt(name: String): Option[GetField] = Option(field(name))

  def fields: Map[String, GetField] = Option(original.getFields).fold(Map.empty[String, GetField])(_.asScala.toMap)

  def id: String = original.getId
  def index: String = original.getIndex

  def source: Map[String, AnyRef] = Option(original.getSource).map(_.asScala.toMap).getOrElse(Map.empty)
  def sourceAsBytes: Array[Byte] = original.getSourceAsBytes
  def sourceAsString: String = original.getSourceAsString

  def `type`: String = original.getType
  def version: Long = original.getVersion

  def isExists: Boolean = original.isExists
  def isSourceEmpty: Boolean = original.isSourceEmpty
  def iterator: Iterator[GetField] = original.iterator.asScala

  def to[T](implicit read: HitReader[T], manifest: Manifest[T]): Or[T, ErrorMessage] = read.from(new GetHit(original))
}

case class RichGetField(original: GetField) extends AnyVal {

  import scala.collection.JavaConverters._

  def name: String = original.getName
  def value: AnyRef = original.getValue
  def values: Seq[AnyRef] = original.getValues.asScala
  def isMetadataField: Boolean = original.isMetadataField
  def iterator: Iterator[AnyRef] = original.iterator.asScala
}

class GetHit(resp: GetResponse) extends Hit {

  import scala.collection.JavaConverters._

  override def id: String = resp.getId
  override def index: String = resp.getIndex
  override def source: Map[String, AnyRef] = resp.getSource.asScala.toMap
  override def sourceAsBytes: Array[Byte] = resp.getSourceAsBytes
  override def sourceAsString: String = resp.getSourceAsString
  override def isExists: Boolean = resp.isExists
  override def isEmpty: Boolean = resp.isSourceEmpty
  override def `type`: String = resp.getType
  override def version: Long = resp.getVersion
  override def field(name: String): HitField = new GetHitField(resp.getField(name))
  override def fieldOpt(name: String): Option[HitField] = Option(resp.getField(name)).map(new GetHitField(_))
  override def fields: Map[String, HitField] = resp.getFields.asScala.toMap.map { case (key, value) =>
    key -> new GetHitField(value)
  }
}

class GetHitField(java: GetField) extends HitField {

  import scala.collection.JavaConverters._

  override def name: String = java.getName
  override def value: AnyRef = java.getValue
  override def values: Seq[AnyRef] = java.getValues.asScala
  override def isMetadataField: Boolean = java.isMetadataField
}
