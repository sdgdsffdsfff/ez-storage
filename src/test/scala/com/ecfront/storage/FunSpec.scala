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
    ResourceService.save(res, null)
    assert(ResourceService.getById("res1", null).get.getName == "资源1")
    res.name = "资源new"
    ResourceService.update("res1", res, null)
    assert(ResourceService.getById("res1", null).get.getName == "资源new")
    res.id = "res2"
    res.name = "资源2"
    ResourceService.save(res, null)
    res.id = "res3"
    res.name = "资源3"
    ResourceService.save(res, null)
    res.id = "res4"
    res.name = "resource4"
    ResourceService.save(res, null)
    assert(ResourceService.findAll(null).get.size == 4)
    val page = ResourceService.pageByCondition("name like '资源%'", 1, 2, null).get
    assert(page.pageTotal == 2 && page.pageSize == 2 && page.recordTotal == 3 && page.results.size == 2)
    ResourceService.deleteById("res1", null)
    assert(ResourceService.findAll(null).get.size == 3)
  }

  test("ManyToMany测试") {
    val res = Resource()
    res.id = "res1"
    res.name = "资源1"
    ResourceService.save(res, null)
    res.id = "res2"
    res.name = "资源2"
    ResourceService.save(res, null)

    val account = Account()
    account.id = "user1"

    var role = Role()
    role.id = "role1"
    role.name = "admin"
    RoleService.save(role, null)

    role = RoleService.getById("role1", null).get
    assert(role.id == "role1")
    assert(role.resourceIds.size == 0)

    //====================update&get======================
    role.resourceIds = List("res1")
    RoleService.update("role1", role, null)
    assert(RoleService.getById("role1", null).get.resourceIds(0) == "res1")
    role.resourceIds = List("res1", "res2")
    RoleService.update("role1", role, null)
    assert(RoleService.getById("role1", null).get.resourceIds(1) == "res2")
    role.resourceIds = List("res1")
    RoleService.update("role1", role, null)
    assert(RoleService.getById("role1", null).get.resourceIds.size == 1)
    role.resourceIds = null
    RoleService.update("role1", role, null)
    assert(RoleService.getById("role1", null).get.resourceIds.size == 0)
    //====================save&get======================
    role.id = "role2"
    role.name = "user"
    role.resourceIds = List("res1", "res2")
    RoleService.save(role, null)
    assert(RoleService.getById("role2", null).get.resourceIds(1) == "res2")
    //====================delete&get======================
    RoleService.deleteById("res1", null)
    assert(RoleService.getById("role2", null).get.resourceIds.size == 2)
    account.roleIds = List("role1")
    AccountService.save(account, null)
    assert(AccountService.getById("user1", null).get.roleIds.size == 1)
    RoleService.deleteById("role1", null)
    assert(AccountService.getById("user1", null).get.roleIds.size == 0)
  }

}

object RoleService extends JDBCStorable[Role, Void]

object AccountService extends JDBCStorable[Account, Void]

object ResourceService extends JDBCStorable[Resource, Void]


