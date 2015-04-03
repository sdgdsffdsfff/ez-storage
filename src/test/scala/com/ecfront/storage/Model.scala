package com.ecfront.storage

import scala.beans.BeanProperty

@Entity(desc = "角色表")
case class Role() extends IdModel {
  @Desc("code")
  @Index
  @Unique
  @BeanProperty var code: String = _
  @Desc("name")
  @Index
  @BeanProperty var name: String = _
  @ManyToMany(mapping = "Resource", master = true, fetch = true)
  @BeanProperty var resourceIds: List[String] = List()
  @ManyToMany(mapping = "Account", master = false, fetch = false)
  @BeanProperty var accountIds: List[String] = List()
}

@Entity("资源表")
case class Resource() extends IdModel {
  @BeanProperty @Text var name: String = _
}

@Entity("账户表")
case class Account() extends IdModel {
  @ManyToMany(mapping = "Role", master = true, fetch = true)
  @BeanProperty var roleIds: List[String] = List()
}

abstract class IdModel {
  @BeanProperty @Id var id: String = _
}

object IdModel {
  val ID_FLAG = "id"
}
