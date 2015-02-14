import com.wordnik.swagger.util.Json
import io.swagger.parser._
import io.swagger.parser.{Swagger20Parser, SwaggerParser}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

@RunWith(classOf[JUnitRunner])
class SwaggerReaderTestTest extends FlatSpec with Matchers {
  val m = Json.mapper()

  it should "read the uber api" in {
    val parser = new SwaggerParser()
    val swagger = parser.read("./src/test/resources/uber.json")
  }
}
