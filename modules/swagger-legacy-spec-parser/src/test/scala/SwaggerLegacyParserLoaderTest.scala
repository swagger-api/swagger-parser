import com.wordnik.swagger.util.Json
import com.wordnik.swagger.parser._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

@RunWith(classOf[JUnitRunner])
class SwaggerLegacyParserLoaderTest extends FlatSpec with Matchers {
  val m = Json.mapper()

  it should "verify the default parser extension" in {
    val parser = new SwaggerParser()
    val extensions = parser.getExtensions()
    extensions.size should be > (0)
    println(extensions)
  }
}