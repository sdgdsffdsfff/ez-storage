package com.ecfront.storage

import com.ecfront.common.JsonHelper

import scala.annotation.StaticAnnotation
import scala.beans.BeanProperty
import scala.collection.mutable.ArrayBuffer

case class PageModel[M](
                         @BeanProperty var pageNumber: Long,
                         @BeanProperty var pageSize: Long,
                         @BeanProperty var pageTotal: Long,
                         @BeanProperty var recordTotal: Long,
                         @BeanProperty var results: List[M]
                         )

object PageModel {

  val PAGE_NUMBER_FLAG = "pageNumber"
  val PAGE_SIZE_FLAG = "pageSize"

  def toPage[M](str: String, modelClazz: Class[M]): PageModel[M] = {
    val tmp = JsonHelper.toJson(str)
    val res = tmp.get("results").elements()
    val results = ArrayBuffer[M]()
    while (res.hasNext) {
      results += JsonHelper.toObject(res.next(), modelClazz)
    }
    PageModel(
      tmp.get("pageNumber").asLong(),
      tmp.get("pageSize").asLong(),
      tmp.get("pageTotal").asLong(),
      tmp.get("recordTotal").asLong(),
      results.toList
    )
  }
}

case class Entity(idField: String) extends StaticAnnotation

