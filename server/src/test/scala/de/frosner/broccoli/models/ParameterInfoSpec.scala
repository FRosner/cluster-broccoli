package de.frosner.broccoli.models

import de.frosner.broccoli.auth.{Account, Role}
import de.frosner.broccoli.models.ListProvider.{
  StaticDoubleListProvider,
  StaticIntListProvider,
  StaticStringListProvider,
  UserOEListProvider
}
import de.frosner.broccoli.models.ParameterInfo.{parameterInfoReads, parameterInfoWrites}
import org.specs2.mutable.Specification
import play.api.libs.json.{JsArray, JsBoolean, JsNumber, JsString, Json}

class ParameterInfoSpec extends Specification {

  "Service status JSON serialization" should {

    "work" in {
      val info = ParameterInfo(id = "i",
                               name = None,
                               default = Some(StringParameterValue("d")),
                               secret = Some(false),
                               `type` = ParameterType.String,
                               orderIndex = None)
      Json.fromJson(Json.toJson(info)).get === info
    }
  }

  "ParameterInfo Serialization" should {
    "work for integer list" in {
      val info =
        ParameterInfo(id = "i",
                      name = None,
                      default = None,
                      secret = Some(false),
                      `type` = ParameterType.List(StaticIntListProvider(List(1, 2, 3, 4))),
                      orderIndex = None)
      Json.fromJson(Json.toJson(info)).get == info
    }

    "work for double list" in {
      val info =
        ParameterInfo(id = "i",
                      name = None,
                      default = None,
                      secret = Some(false),
                      `type` = ParameterType.List(StaticDoubleListProvider(List(1.1, 2.2, 3.3, 4.4))),
                      orderIndex = None)
      Json.fromJson(Json.toJson(info)).get == info
    }

    "work for string list" in {
      val info =
        ParameterInfo(id = "i",
                      name = None,
                      default = None,
                      secret = Some(false),
                      `type` = ParameterType.List(StaticStringListProvider(List("1", "2", "3", "4"))),
                      orderIndex = None)
      Json.fromJson(Json.toJson(info)).get == info
    }

    "work for UserOEProvider list" in {
      val info =
        ParameterInfo(id = "i",
                      name = None,
                      default = None,
                      secret = Some(false),
                      `type` = ParameterType.List(UserOEListProvider),
                      orderIndex = None)
      Json.fromJson(Json.toJson(info)).get == info
    }

    "work for int list with API writes" in {
      val values = List(1, 2, 3, 4)
      val info =
        ParameterInfo(id = "i",
                      name = None,
                      default = None,
                      secret = Some(false),
                      `type` = ParameterType.List(StaticIntListProvider(values)),
                      orderIndex = None)
      implicit val account: Account = Account.anonymous

      Json.obj(
        "id" -> JsString("i"),
        "secret" -> JsBoolean(false),
        "type" -> Json.obj(
          "name" -> "intList",
          "values" -> JsArray(values.map(JsNumber(_)))
        )
      ) == Json.toJson(info)(ParameterInfo.parameterInfoApiWrites)
    }

    "work for double list with API writes" in {
      val values = List(1.1, 2.2, 3.3, 4.4)
      val info =
        ParameterInfo(id = "i",
                      name = None,
                      default = None,
                      secret = Some(false),
                      `type` = ParameterType.List(StaticDoubleListProvider(values)),
                      orderIndex = None)
      implicit val account: Account = Account.anonymous

      Json.obj(
        "id" -> JsString("i"),
        "secret" -> JsBoolean(false),
        "type" -> Json.obj(
          "name" -> "decimalList",
          "values" -> JsArray(values.map(JsNumber(_)))
        )
      ) == Json.toJson(info)(ParameterInfo.parameterInfoApiWrites)
    }

    "work for string list with API writes" in {
      val values = List("1", "2", "3", "4")
      val info =
        ParameterInfo(id = "i",
                      name = None,
                      default = None,
                      secret = Some(false),
                      `type` = ParameterType.List(StaticStringListProvider(values)),
                      orderIndex = None)
      implicit val account: Account = Account.anonymous

      Json.obj(
        "id" -> JsString("i"),
        "secret" -> JsBoolean(false),
        "type" -> Json.obj(
          "name" -> "stringList",
          "values" -> JsArray(values.map(JsString))
        )
      ) == Json.toJson(info)(ParameterInfo.parameterInfoApiWrites)
    }

    "work for UserOEProvider API writes" in {
      val info =
        ParameterInfo(id = "i",
                      name = None,
                      default = None,
                      secret = Some(false),
                      `type` = ParameterType.List(UserOEListProvider),
                      orderIndex = None)
      implicit val account: Account = Account("testoe-admin", "testoe-*", Role.Administrator)

      Json.obj(
        "id" -> JsString("i"),
        "secret" -> JsBoolean(false),
        "type" -> Json.obj(
          "name" -> "stringList",
          "values" -> JsArray(List("testoe").map(JsString))
        )
      ) == Json.toJson(info)(ParameterInfo.parameterInfoApiWrites)
    }
  }

}
