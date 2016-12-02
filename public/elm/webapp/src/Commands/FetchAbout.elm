module Commands.FetchAbout exposing (fetchAbout)

import Http
import Models.Resources.AboutInfo exposing (AuthInfo, AboutInfo, ProjectInfo, ScalaInfo, SbtInfo, UserInfo)
import Commands.Fetch exposing (apiBaseUrl)
import Json.Decode as Decode exposing (field)
import Updates.Messages exposing (UpdateAboutInfoMsg(..))

fetchUrl = String.concat [ apiBaseUrl, "about" ]

fetchAbout : Cmd UpdateAboutInfoMsg
fetchAbout =
    Http.get fetchUrl aboutInfoDecoder
        |> Http.send FetchAbout

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
