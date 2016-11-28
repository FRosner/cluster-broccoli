module Templates.Update exposing (..)

import Templates.Messages exposing (Msg(..))
import Templates.Models exposing (..)
import Dict exposing (..)

update : Msg -> Model -> (Model, Cmd Msg)
update message oldModel =
  case message of
    FetchTemplates (Ok newTemplates) ->
      ( newTemplates
        |> List.map (\t -> (t.id, (TemplateWithForms t Nothing)))
        |> Dict.fromList
      , Cmd.none
      )

    FetchTemplates (Err error) ->
      ( Dict.empty
      , Cmd.none
      )

    ShowNewInstanceForm template ->
      ( Dict.update template.id (updateTemplateForm (Just (NewInstanceForm template))) oldModel
      , Cmd.none
      )

    HideNewInstanceForm template ->
      ( Dict.update template.id (updateTemplateForm Nothing) oldModel
      , Cmd.none
      )

updateTemplateForm : Maybe NewInstanceForm -> Maybe TemplateWithForms -> Maybe TemplateWithForms
updateTemplateForm maybeNewForm maybeOldTemplateWithForms =
  case maybeOldTemplateWithForms of
    Just templateWithForms ->
      Just { templateWithForms | newInstanceForm = maybeNewForm }
    Nothing ->
      maybeOldTemplateWithForms
