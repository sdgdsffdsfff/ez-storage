package com.ecfront.storage

trait SimpleJDBCStorable[M <: AnyRef] extends JDBCStorable[M, Void] {

  override protected def __appendAuth(request: Void=null): String = super.__appendAuth(request)

  override def __deleteAll(request: Void=null): Option[List[String]] = super.__deleteAll(request)

  override def __deleteAllWithoutTransaction(request: Void=null): Option[List[String]] = super.__deleteAllWithoutTransaction(request)

  override def __deleteByCondition(condition: String, request: Void=null): Option[List[String]] = super.__deleteByCondition(condition, request)

  override def __deleteByConditionWithoutTransaction(condition: String, request: Void=null): Option[List[String]] = super.__deleteByConditionWithoutTransaction(condition, request)

  override def __deleteById(id: String, request: Void=null): Option[String] = super.__deleteById(id, request)

  override def __deleteByIdWithoutTransaction(id: String, request: Void=null): Option[String] = super.__deleteByIdWithoutTransaction(id, request)

  override def __findAll(request: Void=null): Option[List[M]] = super.__findAll(request)

  override def __findByCondition(condition: String, request: Void=null): Option[List[M]] = super.__findByCondition(condition, request)

  override def __getByCondition(condition: String, request: Void=null): Option[M] = super.__getByCondition(condition, request)

  override def __getById(id: String, request: Void=null): Option[M] = super.__getById(id, request)

  override def __pageAll(pageNumber: Long, pageSize: Long, request: Void=null): Option[PageModel[M]] = super.__pageAll(pageNumber, pageSize, request)

  override def __pageByCondition(condition: String, pageNumber: Long, pageSize: Long, request: Void): Option[PageModel[M]] = super.__pageByCondition(condition, pageNumber, pageSize, request)

  override def __save(model: M, request: Void=null): Option[String] = super.__save(model, request)

  override def __saveWithoutTransaction(model: M, request: Void=null): Option[String] = super.__saveWithoutTransaction(model, request)

  override def __update(id: String, model: M, request: Void=null): Option[String] = super.__update(id, model, request)

  override def __updateWithoutTransaction(id: String, model: M, request: Void=null): Option[String] = super.__updateWithoutTransaction(id, model, request)
}
