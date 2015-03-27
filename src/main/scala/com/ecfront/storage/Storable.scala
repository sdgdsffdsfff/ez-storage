package com.ecfront.storage

import java.lang.reflect.ParameterizedType
import java.util.UUID

import com.ecfront.common.{BeanHelper, Ignore}
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
 * 存储接口
 * @tparam M 要操作的实体对象
 * @tparam Q 请求附加对象，如可在此对象中加入请求的用户名、角色，重写_appendAuth方法实现权限控制
 */
trait Storable[M <: AnyRef, Q <: AnyRef] extends LazyLogging {

  protected val _modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]
  protected val _tableName = _modelClazz.getSimpleName
  protected val _classAnnotation = BeanHelper.getClassAnnotation[Entity](_modelClazz).getOrElse {
    logger.error("The storage entity must has Entity annotation.")
    null
  }
  protected val _idField = _classAnnotation.idField
  protected val _allAnnotations = BeanHelper.findFieldAnnotations(_modelClazz)
  protected val _allFields = BeanHelper.findFields(_modelClazz, filterAnnotations = Seq())
  protected val _ignoreFields = _allFields.filter {
    field =>
      _allAnnotations.filter(_.fieldName == field._1).exists {
        ann =>
          ann.annotation.getClass == classOf[Ignore] || ann.annotation.getClass == classOf[ManyToMany]
      }
  }.map(_._1).toList
  protected val _persistentFields = _allFields.filter(field => !_ignoreFields.contains(field._1))
  protected val _manyToManyFields = _allAnnotations.filter(_.annotation.isInstanceOf[ManyToMany]).map {
    field =>
      (field.annotation.asInstanceOf[ManyToMany], field.fieldName)
  }


  logger.info( """Create Storage Service: model: %s""".format(_modelClazz.getSimpleName))

  protected def _init(modelClazz: Class[M]): Unit

  _init(_modelClazz)

  protected def _getMapValue(model: M): Map[String, Any] = {
    BeanHelper.findValues(model, _ignoreFields)
  }

  def _getById(id: String, request: Q): Option[M] = {
    _doGetById(id, request)
  }

  protected def _doGetById(id: String, request: Q): Option[M]

  def _getByCondition(condition: String, request: Q): Option[M] = {
    _doGetByCondition(condition, request)
  }

  protected def _doGetByCondition(condition: String, request: Q): Option[M]

  def _findAll(request: Q): Option[List[M]] = {
    _doFindAll(request)
  }

  protected def _doFindAll(request: Q): Option[List[M]]

  def _findByCondition(condition: String, request: Q): Option[List[M]] = {
    _doFindByCondition(condition, request)
  }

  protected def _doFindByCondition(condition: String, request: Q): Option[List[M]]

  def _pageAll(pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]] = {
    _doPageAll(pageNumber, pageSize, request)
  }

  protected def _doPageAll(pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]]

  def _pageByCondition(condition: String, pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]] = {
    _doPageByCondition(condition, pageNumber, pageSize, request)
  }

  protected def _doPageByCondition(condition: String, pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]]

  def _save(model: M, request: Q): Option[String] = {
    _doSave(model, request)
  }

  protected def _doSave(model: M, request: Q): Option[String]

  def _saveWithoutTransaction(model: M, request: Q): Option[String] = {
    val idValue = _getIdValue(model)
    if (idValue == null || idValue.isEmpty) {
      _setValueByField(model, _idField, UUID.randomUUID().toString)
    }
    _doSaveWithoutTransaction(model, request)
  }

  protected def _doSaveWithoutTransaction(model: M, request: Q): Option[String]

  def _update(id: String, model: M, request: Q): Option[String] = {
    _doUpdate(id, model, request)
  }

  protected def _doUpdate(id: String, model: M, request: Q): Option[String]

  def _updateWithoutTransaction(id: String, model: M, request: Q): Option[String] = {
    val savedModel = _doGetById(id, request).get
    BeanHelper.copyProperties(savedModel, model)
    _doUpdateWithoutTransaction(id, savedModel, request)
  }

  protected def _doUpdateWithoutTransaction(id: String, model: M, request: Q): Option[String]

  def _deleteById(id: String, request: Q): Option[String] = {
    _doDeleteById(id, request)
  }

  protected def _doDeleteById(id: String, request: Q): Option[String]

  def _deleteByIdWithoutTransaction(id: String, request: Q): Option[String] = {
    _doDeleteByIdWithoutTransaction(id, request)
  }

  protected def _doDeleteByIdWithoutTransaction(id: String, request: Q): Option[String]

  def _deleteByCondition(condition: String, request: Q): Option[List[String]] = {
    _doDeleteByCondition(condition, request)
  }

  protected def _doDeleteByCondition(condition: String, request: Q): Option[List[String]]

  def _deleteAllWithoutTransaction(request: Q): Option[List[String]] = {
    _doDeleteAllWithoutTransaction(request)
  }

  protected def _doDeleteAllWithoutTransaction(request: Q): Option[List[String]]

  def _deleteAll(request: Q): Option[List[String]] = {
    _doDeleteAll(request)
  }

  protected def _doDeleteAll(request: Q): Option[List[String]]

  def _deleteByConditionWithoutTransaction(condition: String, request: Q): Option[List[String]] = {
    _doDeleteByConditionWithoutTransaction(condition, request)
  }

  protected def _doDeleteByConditionWithoutTransaction(condition: String, request: Q): Option[List[String]]

  protected def _appendAuth(request: Q): String

  protected def _getIdValue(model: AnyRef): String = {
    val idValue = _getValueByField(model, _idField)
    if (idValue == null) null else idValue.asInstanceOf[String].trim
  }

  protected def _getValueByField(model: AnyRef, fieldName: String): Any = {
    BeanHelper.getValue(model, fieldName).orNull
  }

  protected def _setValueByField(model: AnyRef, fieldName: String, value: Any): Unit = {
    BeanHelper.setValue(model, fieldName, value)
  }

}



