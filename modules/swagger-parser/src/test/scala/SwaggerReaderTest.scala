import io.swagger.parser.SwaggerParser
import io.swagger.util.Json
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner

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
}
