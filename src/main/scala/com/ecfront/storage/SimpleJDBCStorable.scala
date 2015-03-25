package com.ecfront.storage

trait SimpleJDBCStorable[M <: AnyRef] extends JDBCStorable[M, Void] {

  override protected def _appendAuth(request: Void=null): String = super._appendAuth(request)

  override def _deleteAll(request: Void=null): Option[List[String]] = super._deleteAll(request)

  override def _deleteAllWithoutTransaction(request: Void=null): Option[List[String]] = super._deleteAllWithoutTransaction(request)

  override def _deleteByCondition(condition: String, request: Void=null): Option[List[String]] = super._deleteByCondition(condition, request)

  override def _deleteByConditionWithoutTransaction(condition: String, request: Void=null): Option[List[String]] = super._deleteByConditionWithoutTransaction(condition, request)

  override def _deleteById(id: String, request: Void=null): Option[String] = super._deleteById(id, request)

  override def _deleteByIdWithoutTransaction(id: String, request: Void=null): Option[String] = super._deleteByIdWithoutTransaction(id, request)

  override def _findAll(request: Void=null): Option[List[M]] = super._findAll(request)

  override def _findByCondition(condition: String, request: Void=null): Option[List[M]] = super._findByCondition(condition, request)

  override def _getByCondition(condition: String, request: Void=null): Option[M] = super._getByCondition(condition, request)

  override def _getById(id: String, request: Void=null): Option[M] = super._getById(id, request)

  override protected def _init(modelClazz: Class[M]): Unit = super._init(modelClazz)

  override def _pageAll(pageNumber: Long, pageSize: Long, request: Void=null): Option[PageModel[M]] = super._pageAll(pageNumber, pageSize, request)

  override def _pageByCondition(condition: String, pageNumber: Long, pageSize: Long, request: Void): Option[PageModel[M]] = super._pageByCondition(condition, pageNumber, pageSize, request)

  override def _save(model: M, request: Void=null): Option[String] = super._save(model, request)

  override def _saveWithoutTransaction(model: M, request: Void=null): Option[String] = super._saveWithoutTransaction(model, request)

  override def _update(id: String, model: M, request: Void=null): Option[String] = super._update(id, model, request)

  override def _updateWithoutTransaction(id: String, model: M, request: Void=null): Option[String] = super._updateWithoutTransaction(id, model, request)
}
