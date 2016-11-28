module View exposing (..)

import Html exposing (Html, div, text)
import Messages exposing (Msg(..))
import Models exposing (Model)
import Players.List
import About.View
import Templates.View


view : Model -> Html Msg
view model =
    div []
        [ page model ]


page : Model -> Html Msg
page model =
  div []
      [ Html.map AboutMsg (About.View.projectName model.aboutInfo)
      , Html.map TemplatesMsg (Templates.View.view model.templatesModel)
      ]
