module About.Commands exposing (..)

import Http
import Json.Decode as Decode exposing (field)
import About.Models exposing (..)
import About.Messages exposing (..)


fetch : Cmd Msg
fetch =
    Http.get fetchUrl aboutInfoDecoder
        |> Http.send FetchAbout


fetchUrl : String
fetchUrl =
    "http://localhost:9000/api/v1/about"

projectInfoDecoder =
  Decode.map2 ProjectInfo
    (field "name" Decode.string)
    (field "version" Decode.string)

scalaInfoDecoder =
  Decode.map ScalaInfo
    (field "version" Decode.string)

sbtInfoDecoder =
  Decode.map SbtInfo
    (field "version" Decode.string)

userInfoDecoder =
  Decode.map3 UserInfo
    (field "name" Decode.string)
    (field "role" Decode.string)
    (field "instanceRegex" Decode.string)

authInfoDecoder =
  Decode.map2 AuthInfo
    (field "enabled" Decode.bool)
    (field "user" userInfoDecoder)

aboutInfoDecoder =
  Decode.map4 AboutInfo
    (field "project" projectInfoDecoder)
    (field "scala" scalaInfoDecoder)
    (field "sbt" sbtInfoDecoder)
    (field "auth" authInfoDecoder)
