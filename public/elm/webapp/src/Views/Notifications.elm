module Views.Notifications exposing (view)

import Html exposing (..)
import Html.Attributes exposing (class)
import Models.Ui.Notifications exposing (Error, Errors)
import Messages exposing (AnyMsg)

view : Errors -> Html AnyMsg
view errors =
  div
    []
    (List.map errorAlert errors)

errorAlert : Error -> Html AnyMsg
errorAlert error =
  div
    [ class "alert alert-danger animated fadeIn" ]
    [ text error ]
