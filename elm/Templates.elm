module Templates exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)

type alias Template =
  { id : String
  , description : String
  }

type alias Templates = List Template

initialTemplates : Templates
initialTemplates =
  [ Template "1" "wow"
  , Template "2" "awesome" ]

type Msg
  = AddTemplate Template

view : Templates -> Html Msg
view templates =
  div [ class "p2" ]
    [ table []
      [ tbody []
        (List.map templateRow templates)
      ]
    ]

templateRow : Template -> Html Msg
templateRow template =
  tr []
    [ td []
      [ text template.id
      , text template.description
      ]
    ]

update : Msg -> Templates -> ( Templates, Cmd Msg )
update message templates =
    case message of
        AddTemplate template ->
            ( template :: templates, Cmd.none )
