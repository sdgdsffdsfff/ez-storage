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
    ResourceService.__save(res)
    assert(ResourceService.__getById("res1").get.getName == "资源1")
    res.name = "资源new"
    ResourceService.__update("res1", res)
    assert(ResourceService.__getById("res1").get.getName == "资源new")
    res.id = "res2"
    res.name = "资源2"
    ResourceService.__save(res)
    res.id = "res3"
    res.name = "资源3"
    ResourceService.__save(res)
    res.id = "res4"
    res.name = "resource4"
    ResourceService.__save(res)
    assert(ResourceService.__findAll().get.size == 4)
    val page = ResourceService.__pageByCondition("name like '资源%'", 1, 2, null).get
    assert(page.pageTotal == 2 && page.pageSize == 2 && page.recordTotal == 3 && page.results.size == 2)
    ResourceService.__deleteById("res1")
    assert(ResourceService.__findAll().get.size == 3)
  }

  test("ManyToMany测试") {
    val res = Resource()
    res.id = "res1"
    res.name = "资源1"
    ResourceService.__save(res)
    res.id = "res2"
    res.name = "资源2"
    ResourceService.__save(res)

    val account = Account()
    account.id = "user1"

    var role = Role()
    role.id = "role1"
    role.name = "admin"
    RoleService.__save(role, null)

    role = RoleService.__getById("role1", null).get
    assert(role.id == "role1")
    assert(role.resourceIds.size == 0)

    //====================update&get======================
    role.resourceIds = List("res1")
    RoleService.__update("role1", role, null)
    assert(RoleService.__getById("role1", null).get.resourceIds.head == "res1")
    role.resourceIds = List("res1", "res2")
    RoleService.__update("role1", role, null)
    assert(RoleService.__getById("role1", null).get.resourceIds(1) == "res2")
    role.resourceIds = List("res1")
    RoleService.__update("role1", role, null)
    assert(RoleService.__getById("role1", null).get.resourceIds.size == 1)
    role.resourceIds = List()
    RoleService.__update("role1", role, null)
    assert(RoleService.__getById("role1", null).get.resourceIds.size == 0)
    //====================save&get======================
    role.id = "role2"
    role.name = "user"
    role.resourceIds = List("res1", "res2")
    RoleService.__save(role, null)
    assert(RoleService.__getById("role2", null).get.resourceIds(1) == "res2")
    //====================delete&get======================
    RoleService.__deleteById("res1", null)
    assert(RoleService.__getById("role2", null).get.resourceIds.size == 2)
    account.roleIds = List("role1")
    AccountService.__save(account, null)
    assert(AccountService.__getById("user1", null).get.roleIds.size == 1)
    RoleService.__deleteById("role1", null)
    assert(AccountService.__getById("user1", null).get.roleIds.size == 0)
  }

  test("save & update 测试") {
    //auto id test
    val role = Role()
    role.name = "管理员"
    role.code = "admin"
    role.resourceIds = List("res1", "res2")
    val nRole = RoleService.__getById(RoleService.__save(role, null).get, null).get
    assert(nRole.code == "admin")
    assert(nRole.name == "管理员")
    assert(nRole.resourceIds.size == 2)
    //update without null value test
    nRole.code = "root"
    nRole.name = null
    nRole.resourceIds = null
    val nnRole = RoleService.__getById(RoleService.__update(nRole.id, nRole, null).get, null).get
    assert(nnRole.code == "root")
    assert(nnRole.name == "管理员")
    assert(nnRole.resourceIds.size == 2)
  }

}

object RoleService extends JDBCStorable[Role, Void]

object AccountService extends JDBCStorable[Account, Void]

object ResourceService extends SimpleJDBCStorable[Resource]


