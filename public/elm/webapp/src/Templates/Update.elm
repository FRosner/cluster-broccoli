module Templates.Update exposing (..)

import Templates.Messages exposing (Msg(..))
import Templates.Models exposing (..)
import Dict exposing (..)

update : Msg -> Model -> (Model, Cmd Msg)
update message oldModel =
  case message of
    FetchTemplates (Ok newTemplates) ->
      ( newTemplates
        |> List.map (\t -> (t.id, (TemplateWithForms t (NewInstanceForm t False Dict.empty))))
        |> Dict.fromList
      , Cmd.none
      )

    FetchTemplates (Err error) ->
      ( Dict.empty
      , Cmd.none
      )

    ShowNewInstanceForm template ->
      ( Dict.update template.id (showNewInstanceForm True) oldModel
      , Cmd.none
      )

    HideNewInstanceForm template ->
      ( Dict.update template.id (showNewInstanceForm False) oldModel
      , Cmd.none
      )

showNewInstanceForm : Bool -> Maybe TemplateWithForms -> Maybe TemplateWithForms
showNewInstanceForm visibility maybeOldTemplateWithForms =
  case maybeOldTemplateWithForms of
    Just templateWithForms ->
      Just
        { templateWithForms | newInstanceForm = (setVisible templateWithForms.newInstanceForm visibility) }
    Nothing ->
      maybeOldTemplateWithForms
