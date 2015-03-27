package com.ecfront.storage

import com.ecfront.easybi.dbutils.exchange.{DB, DS}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

trait JDBCStorable[M <: AnyRef, Q <: AnyRef] extends Storable[M, Q] {

  override protected def __init(modelClazz: Class[M]): Unit = {
    JDBCStorable.db.createTableIfNotExist(modelClazz.getSimpleName, __persistentFields, __idField)
    __initManyToManyRel(modelClazz)
  }

  private def __initManyToManyRel(clazz: Class[M]): Unit = {
    __allAnnotations.filter(ann => ann.annotation.isInstanceOf[ManyToMany]).foreach {
      ann =>
        val annotation = ann.annotation.asInstanceOf[ManyToMany]
        val (masterFieldName, relFieldName) = __getRelTableFields(annotation)
        JDBCStorable.db.createTableIfNotExist(
          __getRelTableName(annotation),
          Map[String, String](
            masterFieldName -> "String",
            relFieldName -> "String"
          ),
          null)
    }
  }

  override protected def __doGetById(id: String, request: Q): Option[M] = {
    __getByCondition(s"${__idField} = '$id'", request)
  }

  override protected def __doGetByCondition(condition: String, request: Q): Option[M] = {
    val model = Some(JDBCStorable.db.getObject("SELECT * FROM " + __tableName + " WHERE " + condition + __appendAuth(request), __modelClazz))
    if (model != null) {
      __getManyToManyRel(model.get, request)
    }
    model
  }

  private def __getManyToManyRel(model: M, request: Q): Unit = {
    __manyToManyFields.foreach {
      ann =>
        val relTableName = __getRelTableName(ann._1)
        val (masterFieldName, relFieldName) = __getRelTableFields(ann._1)
        val value: List[String] = JDBCStorable.db.find(
          s"SELECT $relFieldName FROM $relTableName WHERE $masterFieldName='${__getIdValue(model)}'"
        ).map(_.get(relFieldName).asInstanceOf[String]).toList
        __setValueByField(model, ann._2, value)
    }
  }

  override protected def __doFindAll(request: Q): Option[List[M]] = {
    __findByCondition("1=1", request)
  }

  override protected def __doFindByCondition(condition: String, request: Q): Option[List[M]] = {
    Some(JDBCStorable.db.findObjects("SELECT * FROM " + __tableName + " WHERE " + condition + __appendAuth(request), __modelClazz).toList)
  }

  override protected def __doPageAll(pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]] = {
    __pageByCondition("1=1", pageNumber, pageSize, request)
  }

  override protected def __doPageByCondition(condition: String, pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]] = {
    val page = JDBCStorable.db.findObjects("SELECT * FROM " + __tableName + " WHERE " + condition + __appendAuth(request), pageNumber, pageSize, __modelClazz)
    Some(PageModel(page.pageNumber, page.pageSize, page.pageTotal, page.recordTotal, page.objects.toList))
  }

  override protected def __doSave(model: M, request: Q): Option[String] = {
    JDBCStorable.db.open()
    val id = __saveWithoutTransaction(model, request)
    JDBCStorable.db.commit()
    id
  }

  override protected def __doSaveWithoutTransaction(model: M, request: Q): Option[String] = {
    JDBCStorable.db.save(__tableName, __getMapValue(model).asInstanceOf[Map[String, AnyRef]])
    val id = __getIdValue(model)
    __saveManyToManyRel(id, model, request)
    Some(id)
  }

  private def __saveManyToManyRel(mainId: String, model: M, request: Q): Unit = {
    __manyToManyFields.foreach {
      ann =>
        val params = ArrayBuffer[Array[AnyRef]]()
        val value = __getValueByField(model, ann._2)
        if (value != null) {
          value.asInstanceOf[Iterable[Any]].foreach {
            value =>
              params += Array(mainId, value.asInstanceOf[AnyRef])
          }
          val (masterFieldName, relFieldName) = __getRelTableFields(ann._1)
          JDBCStorable.db.batch(s"INSERT INTO ${__getRelTableName(ann._1)}  ($masterFieldName,$relFieldName)  VALUES (?,?)", params.toArray)
        }
    }
  }

  override protected def __doUpdate(id: String, model: M, request: Q): Option[String] = {
    JDBCStorable.db.open()
    __updateWithoutTransaction(id, model, request)
    JDBCStorable.db.commit()
    Some(id)
  }

  override protected def __doUpdateWithoutTransaction(id: String, model: M, request: Q): Option[String] = {
    JDBCStorable.db.update(__tableName, id, __getMapValue(model).asInstanceOf[Map[String, AnyRef]])
    __updateManyToManyRel(id, model, request)
    Some(id)
  }

  private def __updateManyToManyRel(mainId: String, model: M, request: Q): Unit = {
    __manyToManyFields.foreach {
      ann =>
        val params = ArrayBuffer[Array[AnyRef]]()
        val relTableName = __getRelTableName(ann._1)
        val (masterFieldName, relFieldName) = __getRelTableFields(ann._1)
        JDBCStorable.db.update(s"DELETE FROM $relTableName WHERE $masterFieldName = ? ", Array(mainId))
        val value = __getValueByField(model, ann._2)
        if (value != null) {
          value.asInstanceOf[Iterable[Any]].foreach {
            value =>
              params += Array(mainId, value.asInstanceOf[AnyRef])
          }
          JDBCStorable.db.batch(s"INSERT INTO $relTableName  ($masterFieldName,$relFieldName)  VALUES (?,?)", params.toArray)
        }
    }
  }

  override protected def __doDeleteById(id: String, request: Q): Option[String] = {
    __deleteByCondition(s"${__idField} = '$id'", request)
    Some(id)
  }

  override protected def __doDeleteByIdWithoutTransaction(id: String, request: Q): Option[String] = {
    __deleteByConditionWithoutTransaction(s"${__idField} = '$id'", request)
    Some(id)
  }

  override protected def __doDeleteAll(request: Q): Option[List[String]] = {
    __deleteByCondition("1=1", request)
  }

  override protected def __doDeleteAllWithoutTransaction(request: Q): Option[List[String]] = {
    __deleteByConditionWithoutTransaction("1=1", request)
  }

  override protected def __doDeleteByCondition(condition: String, request: Q): Option[List[String]] = {
    JDBCStorable.db.open()
    val res = __deleteByConditionWithoutTransaction(condition, request)
    JDBCStorable.db.commit()
    res
  }

  override protected def __doDeleteByConditionWithoutTransaction(condition: String, request: Q): Option[List[String]] = {
    __deleteManyToManyRel(condition, request)
    JDBCStorable.db.update("DELETE FROM " + __tableName + " WHERE " + condition + __appendAuth(request))
    Some(List())
  }

  private def __deleteManyToManyRel(condition: String, request: Q): Unit = {
    __manyToManyFields.foreach {
      ann =>
        val relTableName = __getRelTableName(ann._1)
        val sql = if (condition == "1=1") {
          "DELETE FROM " + relTableName
        } else {
          s"DELETE FROM $relTableName WHERE" +
            s" ${__tableName + "_" + Model.ID_FLAG} in" +
            s" (SELECT ${__idField} FROM ${__tableName} WHERE $condition ${__appendAuth(request)})"
        }
        JDBCStorable.db.update(sql)
    }
  }

  private def __getMappingTableName(annotation: ManyToMany): String = {
    annotation.mapping
  }

  private def __getRelTableName(annotation: ManyToMany): String = {
    val mappingTableName = __getMappingTableName(annotation)
    if (annotation.master) {
      Model.REL_FLAG + "_" + __tableName + "_" + mappingTableName
    } else {
      Model.REL_FLAG + "_" + mappingTableName + "_" + __tableName
    }
  }

  private def __getRelTableFields(annotation: ManyToMany): (String, String) = {
    val mappingTableName = __getMappingTableName(annotation)
    if (annotation.master) {
      (__tableName + "_" + Model.ID_FLAG, mappingTableName + "_" + Model.ID_FLAG)
    } else {
      (mappingTableName + "_" + Model.ID_FLAG, __tableName + "_" + Model.ID_FLAG)
    }
  }

  override protected def __appendAuth(request: Q): String = ""
}

object JDBCStorable extends LazyLogging {

  var db: DB = _

  def init(dbConfig: String): Unit = {
    DS.setConfigPath(dbConfig)
    db = new DB()
  }

}
