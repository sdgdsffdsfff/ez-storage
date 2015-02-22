package com.ecfront.storage

import com.ecfront.easybi.dbutils.exchange.{DB, DS}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

trait JDBCStorable[M <: AnyRef, Q <: AnyRef] extends Storable[M, Q] {

  override protected def init(modelClazz: Class[M]): Unit = {
    JDBCStorable.db.createTableIfNotExist(modelClazz.getSimpleName, persistentFields, idField)
    initManyToManyRel(modelClazz)
  }

  private def initManyToManyRel(clazz: Class[M]): Unit = {
    allAnnotations.filter(ann => ann.annotation.isInstanceOf[ManyToMany]).foreach {
      ann =>
        val annotation = ann.annotation.asInstanceOf[ManyToMany]
        val (masterFieldName, relFieldName) = getRelTableFields(annotation)
        JDBCStorable.db.createTableIfNotExist(
          getRelTableName(annotation),
          Map[String, String](
            masterFieldName -> "String",
            relFieldName -> "String"
          ),
          null)
    }
  }

  override def getById(id: String, request: Q): Option[M] = {
    getByCondition(s"$idField = '$id'", request)
  }

  override def getByCondition(condition: String, request: Q): Option[M] = {
    val model = Some(JDBCStorable.db.getObject("SELECT * FROM " + tableName + " WHERE " + condition + appendAuth(request), modelClazz))
    if (model != null) {
      getManyToManyRel(model.get, request)
    }
    model
  }

  private def getManyToManyRel(model: M, request: Q): Unit = {
    manyToManyFields.foreach {
      ann =>
        val relTableName = getRelTableName(ann._1)
        val (masterFieldName, relFieldName) = getRelTableFields(ann._1)
        val value: List[String] = JDBCStorable.db.find(
          s"SELECT $relFieldName FROM $relTableName WHERE $masterFieldName='${getIdValue(model)}'"
        ).map(_.get(relFieldName).asInstanceOf[String]).toList
        setValueByField(model, ann._2, value)
    }
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
    val id = saveWithoutTransaction(model, request)
    JDBCStorable.db.commit()
    id
  }

  override def saveWithoutTransaction(model: M, request: Q): Option[String] = {
    JDBCStorable.db.save(tableName, getMapValue(model).asInstanceOf[Map[String, AnyRef]])
    val id = getIdValue(model)
    saveManyToManyRel(id, model, request)
    Some(id)
  }

  private def saveManyToManyRel(mainId: String, model: M, request: Q): Unit = {
    manyToManyFields.foreach {
      ann =>
        val params = ArrayBuffer[Array[AnyRef]]()
        val value = getValueByField(model, ann._2)
        if (value != null) {
          value.asInstanceOf[Iterable[Any]].foreach {
            value =>
              params += Array(mainId, value.asInstanceOf[AnyRef])
          }
          val (masterFieldName, relFieldName) = getRelTableFields(ann._1)
          JDBCStorable.db.batch(s"INSERT INTO ${getRelTableName(ann._1)}  ($masterFieldName,$relFieldName)  VALUES (?,?)", params.toArray)
        }
    }
  }

  override def update(id: String, model: M, request: Q): Option[String] = {
    JDBCStorable.db.open()
    updateWithoutTransaction(id, model, request)
    JDBCStorable.db.commit()
    Some(id)
  }

  override def updateWithoutTransaction(id: String, model: M, request: Q): Option[String] = {
    JDBCStorable.db.update(tableName, id, getMapValue(model).asInstanceOf[Map[String, AnyRef]])
    updateManyToManyRel(id, model, request)
    Some(id)
  }

  private def updateManyToManyRel(mainId: String, model: M, request: Q): Unit = {
    manyToManyFields.foreach {
      ann =>
        val params = ArrayBuffer[Array[AnyRef]]()
        val relTableName = getRelTableName(ann._1)
        val (masterFieldName, relFieldName) = getRelTableFields(ann._1)
        JDBCStorable.db.update(s"DELETE FROM $relTableName WHERE $masterFieldName = ? ", Array(mainId))
        val value = getValueByField(model, ann._2)
        if (value != null) {
          value.asInstanceOf[Iterable[Any]].foreach {
            value =>
              params += Array(mainId, value.asInstanceOf[AnyRef])
          }
          JDBCStorable.db.batch(s"INSERT INTO $relTableName  ($masterFieldName,$relFieldName)  VALUES (?,?)", params.toArray)
        }
    }
  }

  override def deleteById(id: String, request: Q): Option[String] = {
    deleteByCondition(s"$idField = '$id'", request)
    Some(id)
  }

  override def deleteByIdWithoutTransaction(id: String, request: Q): Option[String] = {
    deleteByConditionWithoutTransaction(s"$idField = '$id'", request)
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
    val res = deleteByConditionWithoutTransaction(condition, request)
    JDBCStorable.db.commit()
    res
  }

  override def deleteByConditionWithoutTransaction(condition: String, request: Q): Option[List[String]] = {
    deleteManyToManyRel(condition, request)
    JDBCStorable.db.update("DELETE FROM " + tableName + " WHERE " + condition + appendAuth(request))
    Some(List())
  }

  private def deleteManyToManyRel(condition: String, request: Q): Unit = {
    manyToManyFields.foreach {
      ann =>
        val relTableName = getRelTableName(ann._1)
        val sql = if (condition == "1=1") {
          "DELETE FROM " + relTableName
        } else {
          s"DELETE FROM $relTableName WHERE" +
            s" ${tableName + "_" + Model.ID_FLAG} in" +
            s" (SELECT $idField FROM $tableName WHERE $condition ${appendAuth(request)})"
        }
        JDBCStorable.db.update(sql)
    }
  }

  private def getMappingTableName(annotation: ManyToMany): String = {
    annotation.mapping
  }

  private def getRelTableName(annotation: ManyToMany): String = {
    val mappingTableName = getMappingTableName(annotation)
    if (annotation.master) {
      Model.REL_FLAG + "_" + tableName + "_" + mappingTableName
    } else {
      Model.REL_FLAG + "_" + mappingTableName + "_" + tableName
    }
  }

  private def getRelTableFields(annotation: ManyToMany): (String, String) = {
    val mappingTableName = getMappingTableName(annotation)
    if (annotation.master) {
      (tableName + "_" + Model.ID_FLAG, mappingTableName + "_" + Model.ID_FLAG)
    } else {
      (mappingTableName + "_" + Model.ID_FLAG, tableName + "_" + Model.ID_FLAG)
    }
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
