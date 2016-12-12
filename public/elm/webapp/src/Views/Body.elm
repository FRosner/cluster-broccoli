module Views.Body exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Views.TemplateView
import Dict exposing (..)
import Models.Resources.Instance exposing (..)
import Models.Resources.Service exposing (..)
import Models.Resources.Template exposing (TemplateId, Template, addTemplateInstanceString)
import Set exposing (Set)
import Views.NewInstanceForm exposing (view)
import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Utils.HtmlUtils exposing (icon, iconButtonText, iconButton)

view : List Template -> Set TemplateId -> List Instance -> Dict InstanceId (List Service) -> Html UpdateBodyViewMsg
view templates expandedTemplates instances services =
  div
    [ class "container" ]
    (List.map (Views.TemplateView.view expandedTemplates instances services) templates)
