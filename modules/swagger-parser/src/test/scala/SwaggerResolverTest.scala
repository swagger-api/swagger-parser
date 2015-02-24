import com.wordnik.swagger.util.Json

import com.wordnik.swagger.models._
import com.wordnik.swagger.models.properties._
import com.wordnik.swagger.models.parameters._
import com.wordnik.swagger.models.auth.AuthorizationValue

import io.swagger.parser.SwaggerResolver

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class SwaggerResolverTest extends FlatSpec with Matchers {
  it should "resolve a simple remote model property definition" in {
    val swagger = new Swagger()
    swagger.addDefinition(
      "Sample", new ModelImpl()
        .property("remoteRef", new RefProperty("http://petstore.swagger.io/v2/swagger.json#/definitions/Tag")))

    val resolved = new SwaggerResolver().resolve(swagger, null)
    val prop = resolved.getDefinitions().get("Sample").getProperties().get("remoteRef")
    prop.isInstanceOf[RefProperty] should be (true)
    val ref = prop.asInstanceOf[RefProperty]
    ref.get$ref() should equal("#/definitions/Tag")
    swagger.getDefinitions().get("Tag") should not be (null)
  }

  it should "resolve an array remote model property definition" in {
    val swagger = new Swagger()
    swagger.addDefinition(
      "Sample", new ModelImpl()
        .property("remoteRef", new ArrayProperty(new RefProperty("http://petstore.swagger.io/v2/swagger.json#/definitions/Tag"))))

    val resolved = new SwaggerResolver().resolve(swagger, null)
    val prop = resolved.getDefinitions().get("Sample").getProperties().get("remoteRef")
    prop.isInstanceOf[ArrayProperty] should be (true)
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
    val resolved = new SwaggerResolver().resolve(swagger, null)
    val prop = resolved.getDefinitions().get("Sample").getProperties().get("remoteRef")
    prop.isInstanceOf[MapProperty] should be (true)
    val ap = prop.asInstanceOf[MapProperty]
    val ref = ap.getAdditionalProperties().asInstanceOf[RefProperty]
    ref.get$ref() should equal("#/definitions/Tag")
    swagger.getDefinitions().get("Tag") should not be (null)
  }

  it should "resolve operation bodyparam remote refs" in {
    val swagger = new Swagger()
    swagger.path("/fun", new Path()
      .get(new Operation()
        .parameter(new BodyParameter()
          .schema(new RefModel("http://petstore.swagger.io/v2/swagger.json#/definitions/Tag")))))

    val resolved = new SwaggerResolver().resolve(swagger, null)
    val param = swagger.getPaths().get("/fun").getGet().getParameters().get(0).asInstanceOf[BodyParameter]
    val ref = param.getSchema().asInstanceOf[RefModel]
    ref.get$ref() should equal("#/definitions/Tag")
    swagger.getDefinitions().get("Tag") should not be (null)
  }

  it should "resolve response remote refs" in {
    val swagger = new Swagger()
    swagger.path("/fun", new Path()
      .get(new Operation()
        .response(200, new Response()
          .schema(new RefProperty("http://petstore.swagger.io/v2/swagger.json#/definitions/Tag")))))
    val resolved = new SwaggerResolver().resolve(swagger, null)
    val response = swagger.getPaths().get("/fun").getGet().getResponses().get("200")
    val ref = response.getSchema.asInstanceOf[RefProperty]
    ref.get$ref() should equal("#/definitions/Tag")
    swagger.getDefinitions().get("Tag") should not be (null)
  }

  it should "resolve operation parameter remote refs" in {
    val swagger = new Swagger()
    swagger.path("/fun", new Path()
      .get(new Operation()
        .parameter(new RefParameter("#/parameters/SampleParameter"))))

    swagger.parameter("SampleParameter", new QueryParameter()
      .name("skip")
      .property(new IntegerProperty()))

    val resolved = new SwaggerResolver().resolve(swagger, null)
    val params = swagger.getPaths().get("/fun").getGet().getParameters()
    params.size() should be (1)
    val param = params.get(0).asInstanceOf[QueryParameter]
    param.getName() should be ("skip")
  }
}