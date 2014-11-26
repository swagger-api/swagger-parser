import com.wordnik.swagger.util.Json
import io.swagger.parser._
import io.swagger.parser.{Swagger20Parser, SwaggerParser}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

@RunWith(classOf[JUnitRunner])
class SwaggerParserExtensionTest extends FlatSpec with Matchers {
  val m = Json.mapper()

  it should "verify the default parser extension" in {
    val parser = new SwaggerParser()
    val extensions = parser.getExtensions()
    extensions.size should be > (0)
    extensions.get(0).getClass should be (classOf[Swagger20Parser])
  }
}