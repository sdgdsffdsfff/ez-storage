package com.ecfront.storage

import scala.beans.BeanProperty

@Entity(idField = "id")
case class Role() extends IdModel {
  @BeanProperty var code: String = _
  @BeanProperty var name: String = _
  @ManyToMany(mapping = "Resource", master = true, fetch = true)
  @BeanProperty var resourceIds: List[String] = List()
  @ManyToMany(mapping = "Account", master = false, fetch = false)
  @BeanProperty var accountIds: List[String] = List()
}

@Entity(idField = "id")
case class Resource() extends IdModel {
  @BeanProperty var name: String = _
}

@Entity(idField = "id")
case class Account() extends IdModel {
  @ManyToMany(mapping = "Role", master = true, fetch = true)
  @BeanProperty var roleIds: List[String] = List()
}

abstract class IdModel {
  @BeanProperty var id: String = _
}

object IdModel {
  val ID_FLAG = "id"
}
