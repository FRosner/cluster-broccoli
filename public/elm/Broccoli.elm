module Broccoli exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Templates exposing (Templates)

-- MODEL

type alias Model =
  { templates : Templates }

init : ( Model, Cmd Msg )
init =
    ( Model Templates.initialTemplates, Cmd.none )

-- UPDATE

type Msg =
  TemplatesMsg Templates.Msg

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
  case msg of
    TemplatesMsg templatesMsg ->
      let
        ( updatedTemplates, templatesCmd ) =
          Templates.update templatesMsg model.templates
      in
        ( { model | templates = updatedTemplates }, Cmd.map TemplatesMsg templatesCmd )

-- VIEW

view : Model -> Html Msg
view model =
  Html.div []
    [ Html.map TemplatesMsg (Templates.view model.templates)
    ]

-- SUBSCIPTIONS

subscriptions : Model -> Sub Msg
subscriptions model =
  Sub.none

-- APP

main : Program Never Model Msg
main = program
  { init = init
  , view = view
  , update = update
  , subscriptions = subscriptions
  }
