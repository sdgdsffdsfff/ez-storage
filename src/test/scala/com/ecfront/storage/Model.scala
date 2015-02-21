package com.ecfront.storage

import scala.beans.BeanProperty

@Entity(idField = "id")
case class Role() extends IdModel {
  @BeanProperty var code: String = _
  @BeanProperty var name: String = _
  @ManyToMany(mapping = "Resource", master = true, fetch = false)
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
  @ManyToMany(mapping = "Role", master = false, fetch = true)
  @BeanProperty var roleIds: List[String] = _
}

abstract class IdModel {
  @BeanProperty var id: String = _
}

object IdModel {
  val ID_FLAG = "id"
}
