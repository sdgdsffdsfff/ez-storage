package com.ecfront.storage

import org.scalatest.FunSuite


class FunSpec extends FunSuite {

  var testPath = this.getClass.getResource("/").getPath
  if (System.getProperties.getProperty("os.name").toUpperCase.indexOf("WINDOWS") != -1) {
    testPath = testPath.substring(1)
  }

  JDBCStorable.init(testPath)

  test("基础功能测试") {
    val res = Resource()
    res.id = "res1"
    res.name = "资源1"
    ResourceService._save(res)
    assert(ResourceService._getById("res1").get.getName == "资源1")
    res.name = "资源new"
    ResourceService._update("res1", res)
    assert(ResourceService._getById("res1").get.getName == "资源new")
    res.id = "res2"
    res.name = "资源2"
    ResourceService._save(res)
    res.id = "res3"
    res.name = "资源3"
    ResourceService._save(res)
    res.id = "res4"
    res.name = "resource4"
    ResourceService._save(res)
    assert(ResourceService._findAll().get.size == 4)
    val page = ResourceService._pageByCondition("name like '资源%'", 1, 2, null).get
    assert(page.pageTotal == 2 && page.pageSize == 2 && page.recordTotal == 3 && page.results.size == 2)
    ResourceService._deleteById("res1")
    assert(ResourceService._findAll().get.size == 3)
  }

  test("ManyToMany测试") {
    val res = Resource()
    res.id = "res1"
    res.name = "资源1"
    ResourceService._save(res)
    res.id = "res2"
    res.name = "资源2"
    ResourceService._save(res)

    val account = Account()
    account.id = "user1"

    var role = Role()
    role.id = "role1"
    role.name = "admin"
    RoleService._save(role, null)

    role = RoleService._getById("role1", null).get
    assert(role.id == "role1")
    assert(role.resourceIds.size == 0)

    //====================update&get======================
    role.resourceIds = List("res1")
    RoleService._update("role1", role, null)
    assert(RoleService._getById("role1", null).get.resourceIds(0) == "res1")
    role.resourceIds = List("res1", "res2")
    RoleService._update("role1", role, null)
    assert(RoleService._getById("role1", null).get.resourceIds(1) == "res2")
    role.resourceIds = List("res1")
    RoleService._update("role1", role, null)
    assert(RoleService._getById("role1", null).get.resourceIds.size == 1)
    role.resourceIds = null
    RoleService._update("role1", role, null)
    assert(RoleService._getById("role1", null).get.resourceIds.size == 0)
    //====================save&get======================
    role.id = "role2"
    role.name = "user"
    role.resourceIds = List("res1", "res2")
    RoleService._save(role, null)
    assert(RoleService._getById("role2", null).get.resourceIds(1) == "res2")
    //====================delete&get======================
    RoleService._deleteById("res1", null)
    assert(RoleService._getById("role2", null).get.resourceIds.size == 2)
    account.roleIds = List("role1")
    AccountService._save(account, null)
    assert(AccountService._getById("user1", null).get.roleIds.size == 1)
    RoleService._deleteById("role1", null)
    assert(AccountService._getById("user1", null).get.roleIds.size == 0)
  }

}

object RoleService extends JDBCStorable[Role, Void]

object AccountService extends JDBCStorable[Account, Void]

object ResourceService extends SimpleJDBCStorable[Resource]


