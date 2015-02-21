package com.ecfront.storage

import org.scalatest.FunSuite


class FunSpec extends FunSuite {

  var testPath = this.getClass.getResource("/").getPath
  if (System.getProperties.getProperty("os.name").toUpperCase.indexOf("WINDOWS") != -1) {
    testPath = testPath.substring(1)
  }

  JDBCStorable.init(testPath)

  test("功能测试") {
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
}

object ResourceService extends JDBCStorable[Resource, Void]


