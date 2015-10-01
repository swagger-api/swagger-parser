import io.swagger.models.Swagger
import io.swagger.models.parameters.QueryParameter
import io.swagger.parser.SwaggerParser
import io.swagger.util.Json
import org.apache.commons.io.FileUtils

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner

import java.io.File
import java.nio.charset.StandardCharsets

class SwaggerReaderTest extends FlatSpec with Matchers {
  val m = Json.mapper()

  it should "read the uber api" in {
    val parser = new SwaggerParser()
    val swagger = parser.read("./src/test/resources/uber.json")
  }

  it should "read the simple example with minimum values" in {
    val parser = new SwaggerParser()
    val swagger = parser.read("./src/test/resources/sampleWithMinimumValues.yaml")
    val qp = swagger.getPaths().get("/pets").getGet().getParameters().get(0).asInstanceOf[QueryParameter]
    qp.getMinimum() should be (0.0)
  }

  it should "read the simple example with model extensions" in {
    val parser = new SwaggerParser()
    val swagger = parser.read("./src/test/resources/sampleWithMinimumValues.yaml")
    val model = swagger.getDefinitions().get("Cat")
    model.getVendorExtensions().get("x-extension-here") should not be (null)
  }

  it should "detect yaml" in {
    val parser = new SwaggerParser()
    val swagger = parser.read("./src/test/resources/minimal_y")
    swagger.getSwagger() should be("2.0")
  }

  it should "detect json" in {
    val parser = new SwaggerParser()
    val swagger = parser.read("./src/test/resources/minimal_j")
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
