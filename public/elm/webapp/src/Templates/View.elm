module Templates.View exposing (..)

import Html exposing (..)
import Html.Attributes exposing (class)
import Templates.Messages exposing (..)
import Templates.Models exposing (..)


templateList : Templates -> Html Msg
templateList templates =
  div []
    (List.map (\t -> div [] [text t.id]) templates)
