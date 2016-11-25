module About.View exposing (..)

import Html exposing (..)
import Html.Attributes exposing (class)
import About.Messages exposing (..)
import About.Models exposing (..)


projectName : Maybe AboutInfo -> Html Msg
projectName maybeAboutInfo =
  case maybeAboutInfo of
      Just aboutInfo ->
        div []
            [text aboutInfo.projectInfo.name]
      Nothing ->
        div []
            [text "nothing"]
