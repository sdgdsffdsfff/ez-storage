package com.ecfront.storage

import com.ecfront.easybi.dbutils.exchange.{DB, DS}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

trait JDBCStorable[M <: AnyRef, Q <: AnyRef] extends Storable[M, Q] {

  override protected def _init(modelClazz: Class[M]): Unit = {
    JDBCStorable.db.createTableIfNotExist(modelClazz.getSimpleName, persistentFields, _idField)
    initManyToManyRel(modelClazz)
  }

  private def initManyToManyRel(clazz: Class[M]): Unit = {
    _allAnnotations.filter(ann => ann.annotation.isInstanceOf[ManyToMany]).foreach {
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

  override def _getById(id: String, request: Q): Option[M] = {
    _getByCondition(s"${_idField} = '$id'", request)
  }

  override def _getByCondition(condition: String, request: Q): Option[M] = {
    val model = Some(JDBCStorable.db.getObject("SELECT * FROM " + _tableName + " WHERE " + condition + _appendAuth(request), _modelClazz))
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
          s"SELECT $relFieldName FROM $relTableName WHERE $masterFieldName='${_getIdValue(model)}'"
        ).map(_.get(relFieldName).asInstanceOf[String]).toList
        _setValueByField(model, ann._2, value)
    }
  }

  override def _findAll(request: Q): Option[List[M]] = {
    _findByCondition("1=1", request)
  }

  override def _findByCondition(condition: String, request: Q): Option[List[M]] = {
    Some(JDBCStorable.db.findObjects("SELECT * FROM " + _tableName + " WHERE " + condition + _appendAuth(request), _modelClazz).toList)
  }

  override def _pageAll(pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]] = {
    _pageByCondition("1=1", pageNumber, pageSize, request)
  }

  override def _pageByCondition(condition: String, pageNumber: Long, pageSize: Long, request: Q): Option[PageModel[M]] = {
    val page = JDBCStorable.db.findObjects("SELECT * FROM " + _tableName + " WHERE " + condition + _appendAuth(request), pageNumber, pageSize, _modelClazz)
    Some(PageModel(page.pageNumber, page.pageSize, page.pageTotal, page.recordTotal, page.objects.toList))
  }

  override def _save(model: M, request: Q): Option[String] = {
    JDBCStorable.db.open()
    val id = _saveWithoutTransaction(model, request)
    JDBCStorable.db.commit()
    id
  }

  override def _saveWithoutTransaction(model: M, request: Q): Option[String] = {
    JDBCStorable.db.save(_tableName, _getMapValue(model).asInstanceOf[Map[String, AnyRef]])
    val id = _getIdValue(model)
    saveManyToManyRel(id, model, request)
    Some(id)
  }

  private def saveManyToManyRel(mainId: String, model: M, request: Q): Unit = {
    manyToManyFields.foreach {
      ann =>
        val params = ArrayBuffer[Array[AnyRef]]()
        val value = _getValueByField(model, ann._2)
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

  override def _update(id: String, model: M, request: Q): Option[String] = {
    JDBCStorable.db.open()
    _updateWithoutTransaction(id, model, request)
    JDBCStorable.db.commit()
    Some(id)
  }

  override def _updateWithoutTransaction(id: String, model: M, request: Q): Option[String] = {
    JDBCStorable.db.update(_tableName, id, _getMapValue(model).asInstanceOf[Map[String, AnyRef]])
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
        val value = _getValueByField(model, ann._2)
        if (value != null) {
          value.asInstanceOf[Iterable[Any]].foreach {
            value =>
              params += Array(mainId, value.asInstanceOf[AnyRef])
          }
          JDBCStorable.db.batch(s"INSERT INTO $relTableName  ($masterFieldName,$relFieldName)  VALUES (?,?)", params.toArray)
        }
    }
  }

  override def _deleteById(id: String, request: Q): Option[String] = {
    _deleteByCondition(s"${_idField} = '$id'", request)
    Some(id)
  }

  override def _deleteByIdWithoutTransaction(id: String, request: Q): Option[String] = {
    _deleteByConditionWithoutTransaction(s"${_idField} = '$id'", request)
    Some(id)
  }

  override def _deleteAll(request: Q): Option[List[String]] = {
    _deleteByCondition("1=1", request)
  }

  override def _deleteAllWithoutTransaction(request: Q): Option[List[String]] = {
    _deleteByConditionWithoutTransaction("1=1", request)
  }

  override def _deleteByCondition(condition: String, request: Q): Option[List[String]] = {
    JDBCStorable.db.open()
    val res = _deleteByConditionWithoutTransaction(condition, request)
    JDBCStorable.db.commit()
    res
  }

  override def _deleteByConditionWithoutTransaction(condition: String, request: Q): Option[List[String]] = {
    deleteManyToManyRel(condition, request)
    JDBCStorable.db.update("DELETE FROM " + _tableName + " WHERE " + condition + _appendAuth(request))
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
            s" ${_tableName + "_" + Model.ID_FLAG} in" +
            s" (SELECT ${_idField} FROM ${_tableName} WHERE $condition ${_appendAuth(request)})"
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
      Model.REL_FLAG + "_" + _tableName + "_" + mappingTableName
    } else {
      Model.REL_FLAG + "_" + mappingTableName + "_" + _tableName
    }
  }

  private def getRelTableFields(annotation: ManyToMany): (String, String) = {
    val mappingTableName = getMappingTableName(annotation)
    if (annotation.master) {
      (_tableName + "_" + Model.ID_FLAG, mappingTableName + "_" + Model.ID_FLAG)
    } else {
      (mappingTableName + "_" + Model.ID_FLAG, _tableName + "_" + Model.ID_FLAG)
    }
  }

  override protected def _appendAuth(request: Q): String = ""
}

object JDBCStorable extends LazyLogging {

  var db: DB = _

  def init(dbConfig: String): Unit = {
    DS.setConfigPath(dbConfig)
    db = new DB()
  }

}
