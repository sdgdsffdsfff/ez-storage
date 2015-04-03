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

  protected val __modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]
  protected val __tableName = __modelClazz.getSimpleName
  protected val __classAnnotation = BeanHelper.getClassAnnotation[Entity](__modelClazz).getOrElse {
    logger.error("The storage entity must has Entity annotation.")
    null
  }
  protected val __idField = __classAnnotation.idField
  protected val __tableDesc = __classAnnotation.desc
  protected val __allAnnotations = BeanHelper.findFieldAnnotations(__modelClazz)
  protected val __manyToManyFields = __allAnnotations.filter(_.annotation.isInstanceOf[ManyToMany]).map {
    field =>
      (field.annotation.asInstanceOf[ManyToMany], field.fieldName)
  }
  protected val __fieldDesc = __allAnnotations.filter(_.annotation.isInstanceOf[Desc]).map {
    field =>
      field.fieldName -> field.annotation.asInstanceOf[Desc].desc
  }.toMap
  protected val __indexFields = __allAnnotations.filter(_.annotation.isInstanceOf[Index]).map {
    field =>
      field.fieldName
  }.toList
  protected val __uniqueFields = __allAnnotations.filter(_.annotation.isInstanceOf[Unique]).map {
    field =>
      field.fieldName
  }.toList
  protected val __textFields = __allAnnotations.filter(_.annotation.isInstanceOf[Text]).map {
    field =>
      field.fieldName
  }.toList
  protected val __allFields = collection.mutable.Map[String, String]()
  BeanHelper.findFields(__modelClazz, filterAnnotations = Seq()).map {
    item =>
      val ttype = item._1 match {
        case name if __textFields.contains(name) => "text"
        case _ => item._2
      }
      __allFields += item._1 -> ttype
  }
  protected val __ignoreFields = __allFields.filter {
    field =>
      __allAnnotations.filter(_.fieldName == field._1).exists {
        ann =>
          ann.annotation.getClass == classOf[Ignore] || ann.annotation.getClass == classOf[ManyToMany]
      }
  }.map(_._1).toList
  protected val __persistentFields = __allFields.filter(field => !__ignoreFields.contains(field._1))


  logger.info( """Create Storage Service: model: %s""".format(__modelClazz.getSimpleName))

  protected def __init(modelClazz: Class[M]): Unit

  __init(__modelClazz)

  protected def __getMapValue(model: M): Map[String, Any] = {
    BeanHelper.findValues(model, __ignoreFields)
  }

  def __getById(id: String, request: Q): Option[M] = {
    __doGetById(id, request)
  }

  protected def __doGetById(id: String, request: Q): Option[M]

  def __getByCondition(condition: String, request: Q): Option[M] = {
    __doGetByCondition(condition, request)
  }

  protected def __doGetByCondition(condition: String, request: Q): Option[M]

  def __findAll(request: Q): Option[List[M]] = {
    __doFindAll(request)
  }

  protected def __doFindAll(request: Q): Option[List[M]]

  def __findByCondition(condition: String, request: Q): Option[List[M]] = {
    __doFindByCondition(condition, request)
  }

  protected def __doFindByCondition(condition: String, request: Q): Option[List[M]]

  def __pageAll(pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]] = {
    __doPageAll(pageNumber, pageSize, request)
  }

  protected def __doPageAll(pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]]

  def __pageByCondition(condition: String, pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]] = {
    __doPageByCondition(condition, pageNumber, pageSize, request)
  }

  protected def __doPageByCondition(condition: String, pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]]

  def __save(model: M, request: Q): Option[String] = {
    __doSave(model, request)
  }

  protected def __doSave(model: M, request: Q): Option[String]

  def __saveWithoutTransaction(model: M, request: Q): Option[String] = {
    val idValue = __getIdValue(model)
    if (idValue == null || idValue.isEmpty) {
      __setValueByField(model, __idField, UUID.randomUUID().toString)
    }
    __doSaveWithoutTransaction(model, request)
  }

  protected def __doSaveWithoutTransaction(model: M, request: Q): Option[String]

  def __update(id: String, model: M, request: Q): Option[String] = {
    __doUpdate(id, model, request)
  }

  protected def __doUpdate(id: String, model: M, request: Q): Option[String]

  def __updateWithoutTransaction(id: String, model: M, request: Q): Option[String] = {
    val savedModel = __doGetById(id, request).get
    BeanHelper.copyProperties(savedModel, model)
    __doUpdateWithoutTransaction(id, savedModel, request)
  }

  protected def __doUpdateWithoutTransaction(id: String, model: M, request: Q): Option[String]

  def __deleteById(id: String, request: Q): Option[String] = {
    __doDeleteById(id, request)
  }

  protected def __doDeleteById(id: String, request: Q): Option[String]

  def __deleteByIdWithoutTransaction(id: String, request: Q): Option[String] = {
    __doDeleteByIdWithoutTransaction(id, request)
  }

  protected def __doDeleteByIdWithoutTransaction(id: String, request: Q): Option[String]

  def __deleteByCondition(condition: String, request: Q): Option[List[String]] = {
    __doDeleteByCondition(condition, request)
  }

  protected def __doDeleteByCondition(condition: String, request: Q): Option[List[String]]

  def __deleteAllWithoutTransaction(request: Q): Option[List[String]] = {
    __doDeleteAllWithoutTransaction(request)
  }

  protected def __doDeleteAllWithoutTransaction(request: Q): Option[List[String]]

  def __deleteAll(request: Q): Option[List[String]] = {
    __doDeleteAll(request)
  }

  protected def __doDeleteAll(request: Q): Option[List[String]]

  def __deleteByConditionWithoutTransaction(condition: String, request: Q): Option[List[String]] = {
    __doDeleteByConditionWithoutTransaction(condition, request)
  }

  protected def __doDeleteByConditionWithoutTransaction(condition: String, request: Q): Option[List[String]]

  protected def __appendAuth(request: Q): String

  protected def __getIdValue(model: AnyRef): String = {
    val idValue = __getValueByField(model, __idField)
    if (idValue == null) null else idValue.asInstanceOf[String].trim
  }

  protected def __getValueByField(model: AnyRef, fieldName: String): Any = {
    BeanHelper.getValue(model, fieldName).orNull
  }

  protected def __setValueByField(model: AnyRef, fieldName: String, value: Any): Unit = {
    BeanHelper.setValue(model, fieldName, value)
  }

}



