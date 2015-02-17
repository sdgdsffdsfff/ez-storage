package com.ecfront.storage

import com.ecfront.common.Ignore

import scala.beans.BeanProperty

@Entity(idField = "id")
case class Role() extends IdModel {
  @BeanProperty var code: String = _
  @BeanProperty var name: String = _
  @Ignore
  @BeanProperty var resourceIds: List[String] = _
}

@Entity(idField = "id")
case class Resource() extends IdModel {
  @BeanProperty var name: String = _
}

@Entity(idField = "id")
case class Account() extends IdModel {
  @BeanProperty var userId: String = _
  @BeanProperty var password: String = _
  @Ignore
  @BeanProperty var organizationIds: List[String] = _
  @Ignore
  @BeanProperty var roleIds: List[String] = _
}

abstract class IdModel {
  @BeanProperty var id: String = _
}

object IdModel {
  val ID_FLAG = "id"
}
