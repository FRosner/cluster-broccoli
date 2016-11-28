module Templates.View exposing (..)

import Html exposing (..)
import Html.Attributes exposing (class)
import Templates.Messages exposing (..)
import Templates.Models exposing (..)
import FontAwesome
import Color

templateList : Templates -> Html Msg
templateList templates =
  div []
    (List.map (\t -> div [] [FontAwesome.plus_circle Color.black 11, text t.id]) templates)
