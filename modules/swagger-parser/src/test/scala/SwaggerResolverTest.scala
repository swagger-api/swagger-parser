import io.swagger.models._
import io.swagger.models.parameters._
import io.swagger.models.properties._
import io.swagger.parser.SwaggerResolver
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SwaggerResolverTest extends FlatSpec with Matchers {
  it should "resolve a simple remote model property definition" in {
    val swagger = new Swagger()
    swagger.addDefinition(
      "Sample", new ModelImpl()
        .property("remoteRef", new RefProperty("http://petstore.swagger.io/v2/swagger.json#/definitions/Tag")))

    val resolved = new SwaggerResolver(swagger, null).resolve()
    val prop = resolved.getDefinitions().get("Sample").getProperties().get("remoteRef")
    prop.isInstanceOf[RefProperty] should be(true)
    val ref = prop.asInstanceOf[RefProperty]
    ref.get$ref() should equal("#/definitions/Tag")
    swagger.getDefinitions().get("Tag") should not be (null)
  }

  it should "resolve an array remote model property definition" in {
    val swagger = new Swagger()
    swagger.addDefinition(
      "Sample", new ModelImpl()
        .property("remoteRef", new ArrayProperty(new RefProperty("http://petstore.swagger.io/v2/swagger.json#/definitions/Tag"))))

    val resolved = new SwaggerResolver(swagger, null).resolve()
    val prop = resolved.getDefinitions().get("Sample").getProperties().get("remoteRef")
    prop.isInstanceOf[ArrayProperty] should be(true)
    val ap = prop.asInstanceOf[ArrayProperty]
    val ref = ap.getItems().asInstanceOf[RefProperty]
    ref.get$ref() should equal("#/definitions/Tag")
    swagger.getDefinitions().get("Tag") should not be (null)
  }

  it should "resolve an map remote model property definition" in {
    val swagger = new Swagger()
    swagger.addDefinition(
      "Sample", new ModelImpl()
        .property("remoteRef", new MapProperty(new RefProperty("http://petstore.swagger.io/v2/swagger.json#/definitions/Tag"))))
    val resolved = new SwaggerResolver(swagger, null).resolve()
    val prop = resolved.getDefinitions().get("Sample").getProperties().get("remoteRef")
    prop.isInstanceOf[MapProperty] should be(true)
    val ap = prop.asInstanceOf[MapProperty]
    val ref = ap.getAdditionalProperties().asInstanceOf[RefProperty]
    ref.get$ref() should equal("#/definitions/Tag")
    swagger.getDefinitions().get("Tag") should not be (null)
  }

  it should "resolve operation bodyparam remote refs" in {
    val swagger = new Swagger()
    swagger.path("/fun", new PathImpl()
      .get(new Operation()
      .parameter(new BodyParameter()
      .schema(new RefModel("http://petstore.swagger.io/v2/swagger.json#/definitions/Tag")))))

    val resolved = new SwaggerResolver(swagger, null).resolve()
    val param = swagger.getPaths().get("/fun").getGet().getParameters().get(0).asInstanceOf[BodyParameter]
    val ref = param.getSchema().asInstanceOf[RefModel]
    ref.get$ref() should equal("#/definitions/Tag")
    swagger.getDefinitions().get("Tag") should not be (null)
  }


  it should "resolve operation parameter remote refs" in {
    val swagger = new Swagger()
    swagger.path("/fun", new PathImpl()
      .get(new Operation()
      .parameter(new RefParameter("#/parameters/SampleParameter"))))

    swagger.parameter("SampleParameter", new QueryParameter()
      .name("skip")
      .property(new IntegerProperty()))

    val resolved = new SwaggerResolver(swagger, null).resolve()
    val params = swagger.getPaths().get("/fun").getGet().getParameters()
    params.size() should be(1)
    val param = params.get(0).asInstanceOf[QueryParameter]
    param.getName() should be("skip")
  }

  it should "resolve operation body parameter remote refs" in {
    val schema = new ModelImpl()

    val swagger = new Swagger()
    swagger.path("/fun", new PathImpl()
      .get(new Operation()
      .parameter(new RefParameter("#/parameters/SampleParameter"))))

    swagger.path("/times", new PathImpl()
      .get(new Operation()
      .parameter(new RefParameter("#/parameters/SampleParameter"))))

    swagger.parameter("SampleParameter", new BodyParameter()
      .name("skip")
      .schema(schema))


    val resolved = new SwaggerResolver(swagger, null).resolve()
    val params = swagger.getPaths().get("/fun").getGet().getParameters()
    params.size() should be(1)
    val param = params.get(0).asInstanceOf[BodyParameter]
    param.getName() should be("skip")
  }

  it should "resolve response remote refs" in {
    val swagger = new Swagger()
    swagger.path("/fun", new PathImpl()
      .get(new Operation()
      .response(200, new ResponseImpl()
      .schema(new RefProperty("http://petstore.swagger.io/v2/swagger.json#/definitions/Tag")))))
    val resolved = new SwaggerResolver(swagger, null).resolve()
    val response = swagger.getPaths().get("/fun").getGet().getResponses().get("200")
    val ref = response.getSchema.asInstanceOf[RefProperty]
    ref.get$ref() should equal("#/definitions/Tag")
    swagger.getDefinitions().get("Tag") should not be (null)
  }
}

