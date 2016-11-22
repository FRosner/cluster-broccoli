import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onInput)


main =
  Html.beginnerProgram { model = templates, view = overview, update = update }

-- MODEL

type alias Template =
  { id : String
  , description : String
  }

templates : List Template
templates =
  [ (Template "1" "first template"),
    (Template "2" "second template")
  ]

-- UPDATE

type Msg = SetTemplates (List Template)

update : Msg -> List Template -> List Template
update msg oldTemplates =
  case msg of
    SetTemplates templates -> templates

-- VIEW

overview : List Template -> Html Msg
overview templates =
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
