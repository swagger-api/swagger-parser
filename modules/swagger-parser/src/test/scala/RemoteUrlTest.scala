import io.swagger.models.auth.AuthorizationValue
import io.swagger.parser.util.RemoteUrl
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConverters._

class RemoteUrlTest extends FlatSpec with Matchers {
  it should "read a remote URL" in {
    val output = RemoteUrl.urlToString("http://petstore.swagger.io/v2/pet/1", null)
    output should not be (null)
  }

  it should "set a header" in {
    val av = new AuthorizationValue("accept", "application/xml", "header")
    val output = RemoteUrl.urlToString("http://petstore.swagger.io/v2/pet/1", List(av).asJava)
    output.trim.charAt(0) should be('<')
  }

  it should "read yaml" in {
    val output = RemoteUrl.urlToString("http://petstore.swagger.io/v2/swagger.yaml", null)
    output.indexOf("swagger: \"2.0\"") should be > (0)
  }
}