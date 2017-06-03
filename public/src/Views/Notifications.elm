module Views.Notifications exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)

import Models.Ui.Notifications exposing (Error, Errors)
import Utils.HtmlUtils exposing (..)
import Messages exposing (..)
import Updates.Messages exposing (..)

view : Errors -> Html AnyMsg
view errors =
  let indexedErrors =
    List.indexedMap (,) errors
  in
    div
      [ class "container" ]
      (List.map errorAlert indexedErrors)

errorAlert : (Int, Error) -> Html AnyMsg
errorAlert (index, error) =
  div
    [ class "alert alert-danger animated fadeIn" ]
    [ button
      [ type_ "button"
      , class "close"
      , id ( String.concat [ "close-error-", (toString index) ] )
      , onClick ( UpdateErrorsMsg ( CloseError index ) )
      ]
      [ icon "fa fa-times" [] ]
    , text error
    ]
