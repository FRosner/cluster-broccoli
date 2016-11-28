module Main exposing (..)

import Html exposing (Html, div, text, program)
import Messages exposing (Msg(..))
import Models exposing (Model, initialModel)
import Update exposing (update)
import View exposing (view)
import About.Commands
import Templates.Commands

init : ( Model, Cmd Msg )
init =
  ( initialModel
  , Cmd.batch
    [ Cmd.map AboutMsg About.Commands.fetch
    , Cmd.map TemplatesMsg Templates.Commands.fetch
    ]
  )

subscriptions : Model -> Sub Msg
subscriptions model =
  Sub.none

main =
  program
    { init = init
    , view = view
    , update = update
    , subscriptions = subscriptions
    }
