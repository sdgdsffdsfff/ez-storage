package com.ecfront.storage

import com.ecfront.easybi.dbutils.exchange.{DB, DS}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.JavaConversions._

trait JDBCStorable[M <: AnyRef, Q <: AnyRef] extends Storable[M, Q] {

  private def initManyToMany(clazz: Class[M]): Unit = {
    allAnnotations.filter(ann => ann.annotation.isInstanceOf[ManyToMany]).foreach {
      ann =>
        val annotation = ann.annotation.asInstanceOf[ManyToMany]
        if (annotation.master) {
          val relTableName = Model.REL_FLAG + tableName + "_" + annotation.mapping
          JDBCStorable.db.createTableIfNotExist(
            relTableName,
            Map[String, String](
              tableName + "_" + Model.ID_FLAG -> "String",
              annotation.mapping + "_" + Model.ID_FLAG -> "String"
            ),
            null)
        }
    }
  }

  override protected def init(modelClazz: Class[M]): Unit = {
    JDBCStorable.db.createTableIfNotExist(modelClazz.getSimpleName, persistentFields, idField)
    initManyToMany(modelClazz)
  }

  override def getById(id: String, request: Q): Option[M] = {
    getByCondition(s"$idField = '$id'", request)
  }

  override def getByCondition(condition: String, request: Q): Option[M] = {
    Some(JDBCStorable.db.getObject("SELECT * FROM " + tableName + " WHERE " + condition + appendAuth(request), modelClazz))
  }

  override def findAll(request: Q): Option[List[M]] = {
    findByCondition("1=1", request)
  }

  override def findByCondition(condition: String, request: Q): Option[List[M]] = {
    Some(JDBCStorable.db.findObjects("SELECT * FROM " + tableName + " WHERE " + condition + appendAuth(request), modelClazz).toList)
  }

  override def pageAll(pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]] = {
    pageByCondition("1=1", pageNumber, pageSize, request)
  }

  override def pageByCondition(condition: String, pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]] = {
    val page = JDBCStorable.db.findObjects("SELECT * FROM " + tableName + " WHERE " + condition + appendAuth(request), pageNumber, pageSize, modelClazz)
    Some(PageModel(page.pageNumber, page.pageSize, page.pageTotal, page.recordTotal, page.objects.toList))
  }

  override def save(model: M, request: Q): Option[String] = {
    JDBCStorable.db.open()
    saveWithoutTransaction(model, request)
    JDBCStorable.db.commit()
    Some(getIdValue(model))
  }

  override def saveWithoutTransaction(model: M, request: Q): Option[String] = {
    JDBCStorable.db.save(tableName, getMapValue(model).asInstanceOf[Map[String, AnyRef]])
    Some(getIdValue(model))
  }

  override def update(id: String, model: M, request: Q): Option[String] = {
    JDBCStorable.db.open()
    updateWithoutTransaction(id, model, request)
    JDBCStorable.db.commit()
    Some(getIdValue(model))
  }

  override def updateWithoutTransaction(id: String, model: M, request: Q): Option[String] = {
    JDBCStorable.db.update(tableName, id, getMapValue(model).asInstanceOf[Map[String, AnyRef]])
    Some(getIdValue(model))
  }

  override def deleteById(id: String, request: Q): Option[String] = {
    deleteByCondition(s"$idField = '$id'", request)
    Some(id)
  }

  override def deleteByIdWithoutTransaction(id: String, request: Q): Option[String] = {
    deleteByConditionWithoutTransaction( s"""$idField = '$id'""", request)
    Some(id)
  }

  override def deleteAll(request: Q): Option[List[String]] = {
    deleteByCondition("1=1", request)
  }

  override def deleteAllWithoutTransaction(request: Q): Option[List[String]] = {
    deleteByConditionWithoutTransaction("1=1", request)
  }

  override def deleteByCondition(condition: String, request: Q): Option[List[String]] = {
    JDBCStorable.db.open()
    deleteByConditionWithoutTransaction(condition, request)
    JDBCStorable.db.commit()
    Some(List())
  }

  override def deleteByConditionWithoutTransaction(condition: String, request: Q): Option[List[String]] = {
    JDBCStorable.db.update("DELETE FROM " + tableName + " WHERE " + condition + appendAuth(request))
    Some(List())
  }

  override protected def appendAuth(request: Q): String = ""
}

object JDBCStorable extends LazyLogging {

  var db: DB = _

  def init(dbConfig: String): Unit = {
    DS.setConfigPath(dbConfig)
    db = new DB()
  }

}
