module Views.Body exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Views.TemplateView
import Dict exposing (..)
import Models.Resources.Instance exposing (..)
import Models.Resources.Service exposing (..)
import Models.Resources.JobStatus exposing (..)
import Models.Resources.Template exposing (TemplateId, Template, addTemplateInstanceString)
import Models.Ui.InstanceParameterForm exposing (..)
import Models.Ui.BodyUiModel exposing (BodyUiModel)
import Set exposing (Set)
import Views.NewInstanceForm exposing (view)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)

view : List Template -> List Instance -> Dict InstanceId (List Service) -> Dict InstanceId JobStatus -> BodyUiModel -> Html UpdateBodyViewMsg
view templates instances services jobStatuses bodyUiModel =
  div
    [ class "container" ]
    (List.map (Views.TemplateView.view instances services jobStatuses bodyUiModel) templates)
