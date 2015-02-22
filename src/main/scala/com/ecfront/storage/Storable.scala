package com.ecfront.storage

import java.lang.reflect.ParameterizedType

import com.ecfront.common.{BeanHelper, Ignore}
import com.typesafe.scalalogging.slf4j.LazyLogging

trait Storable[M <: AnyRef, Q <: AnyRef] extends LazyLogging {

  protected val _modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]
  protected val _tableName = _modelClazz.getSimpleName
  protected val _classAnnotation = BeanHelper.getClassAnnotation[Entity](_modelClazz).getOrElse {
    logger.error("The storage entity must has Entity annotation.")
    null
  }
  protected val _idField = _classAnnotation.idField
  protected val _allAnnotations = BeanHelper.findFieldAnnotations(_modelClazz)
  protected val _allFields = BeanHelper.findFields(_modelClazz,filterAnnotations=Seq())
  protected val _ignoreFields = _allFields.filter {
    field =>
      _allAnnotations.filter(_.fieldName == field._1).exists {
        ann =>
          ann.annotation.getClass == classOf[Ignore] || ann.annotation.getClass == classOf[ManyToMany]
      }
  }.map(_._1).toList
  protected val persistentFields = _allFields.filter(field => !_ignoreFields.contains(field._1))
  protected val manyToManyFields = _allAnnotations.filter(_.annotation.isInstanceOf[ManyToMany]).map {
    field =>
      (field.annotation.asInstanceOf[ManyToMany], field.fieldName)
  }


  logger.info( """Create Storage Service: model: %s""".format(_modelClazz.getSimpleName))

  protected def _init(modelClazz: Class[M]): Unit

  _init(_modelClazz)

  protected def _getMapValue(model: M): Map[String, Any] = {
    BeanHelper.findValues(model, _ignoreFields)
  }

  def _getById(id: String, request: Q): Option[M]

  def _getByCondition(condition: String, request: Q): Option[M]

  def _findAll(request: Q): Option[List[M]]

  def _findByCondition(condition: String, request: Q): Option[List[M]]

  def _pageAll(pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]]

  def _pageByCondition(condition: String, pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]]

  def _save(model: M, request: Q): Option[String]

  def _saveWithoutTransaction(model: M, request: Q): Option[String]

  def _update(id: String, model: M, request: Q): Option[String]

  def _updateWithoutTransaction(id: String, model: M, request: Q): Option[String]

  def _deleteById(id: String, request: Q): Option[String]

  def _deleteByIdWithoutTransaction(id: String, request: Q): Option[String]

  def _deleteByCondition(condition: String, request: Q): Option[List[String]]

  def _deleteAllWithoutTransaction(request: Q): Option[List[String]]

  def _deleteAll(request: Q): Option[List[String]]

  def _deleteByConditionWithoutTransaction(condition: String, request: Q): Option[List[String]]

  protected def _appendAuth(request: Q): String

  protected def _getIdValue(model: AnyRef): String = {
    _getValueByField(model, _idField).asInstanceOf[String]
  }

  protected def _getValueByField(model: AnyRef, fieldName: String): Any = {
    BeanHelper.getValue(model, fieldName).orNull
  }

  protected def _setValueByField(model: AnyRef, fieldName: String, value: Any): Any = {
    BeanHelper.setValue(model, fieldName, value)
  }

}



