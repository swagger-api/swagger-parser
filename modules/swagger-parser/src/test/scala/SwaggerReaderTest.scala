import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser
import io.swagger.util.Json
import org.apache.commons.io.FileUtils
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner

import java.io.File
import java.nio.charset.StandardCharsets

@RunWith(classOf[JUnitRunner])
class SwaggerReaderTest extends FlatSpec with Matchers {
  val m = Json.mapper()

  it should "read the uber api" in {
    val parser = new SwaggerParser()
    val swagger = parser.read("./src/test/resources/uber.json")
  }

  it should "detect yaml" in {
    val parser = new SwaggerParser()
    val swagger = parser.read("./src/test/resources/minimal_y")
    swagger.getSwagger() should be("2.0")
  }

  it should "detect json" in {
    val parser = new SwaggerParser()
    val swagger = parser.read("./src/test/resources/minimal_y")
    swagger.getSwagger() should be("2.0")
  }

  it should "read the issue 16 resource" in {
    val parser = new SwaggerParser()
    val swagger = parser.read("./src/test/resources/issue_16.yaml")
    // Json.prettyPrint(swagger)
  }

  it should "test https://github.com/swagger-api/swagger-codegen/issues/469" in {
    val parser = new SwaggerParser()
    val swagger = parser.read("./src/test/resources/issue_469.json")
    Json.pretty(swagger.getDefinitions().get("Pet").getExample()) should be(
      """{
  "id" : 12345,
  "name" : "Gorilla"
}"""
    )
  }

  it should "read the issue 59 resource" in {
    val parser = new SwaggerParser()
    val sampleFilePath = "./src/test/resources/uber.json"

    val swaggerFromFile = parser.parse(FileUtils.readFileToString(new File(sampleFilePath), StandardCharsets.UTF_8))
    val swaggerFromString = parser.read(sampleFilePath)

    swaggerFromFile.isInstanceOf[Swagger] should be(true)
    swaggerFromString.isInstanceOf[Swagger] should be(true)
    swaggerFromFile should equal(swaggerFromString)
  }
}
