import com.wordnik.swagger.util.Json

import com.wordnik.swagger.models._
import com.wordnik.swagger.models.properties._
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
}