package com.ecfront.storage

import java.lang.reflect.ParameterizedType

import com.ecfront.common.BeanHelper
import com.typesafe.scalalogging.slf4j.LazyLogging

trait StorageService[M <: AnyRef, Q <: AnyRef] extends LazyLogging {

  protected val modelClazz = this.getClass.getGenericInterfaces()(0).asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[M]]
  protected val tableName = modelClazz.getSimpleName
  protected val classAnnotation = BeanHelper.getClassAnnotation[Entity](modelClazz).getOrElse {
    logger.error("The storage entity must has Entity annotation.")
    null
  }
  protected val idField = classAnnotation.idField

  logger.info( """Create Storage Service: model: %s""".format(modelClazz.getSimpleName))

  protected def init(modelClazz: Class[M]): Unit

  init(modelClazz)

  def getById(id: String, request: Q): Option[M]

  def getByCondition(condition: String, request: Q): Option[M]

  def findAll(request: Q): Option[List[M]]

  def findByCondition(condition: String, request: Q): Option[List[M]]

  def pageAll(pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]]

  def pageByCondition(condition: String, pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]]

  def save(model: M, request: Q): Option[String]

  def saveWithoutTransaction(model: M, request: Q): Option[String]

  def update(id: String, model: M, request: Q): Option[String]

  def updateWithoutTransaction(id: String, model: M, request: Q): Option[String]

  def deleteById(id: String, request: Q): Option[String]

  def deleteByIdWithoutTransaction(id: String, request: Q): Option[String]

  def deleteByCondition(condition: String, request: Q): Option[List[String]]

  def deleteAllWithoutTransaction(request: Q): Option[List[String]]

  def deleteAll(request: Q): Option[List[String]]

  def deleteByConditionWithoutTransaction(condition: String, request: Q): Option[List[String]]

  protected def appendAuth(request: Q): String

  protected def getIdValue(model: AnyRef): String = {
    BeanHelper.getValue(model, idField).getOrElse(null).asInstanceOf[String]
  }

}



